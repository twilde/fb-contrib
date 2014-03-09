/*
 * fb-contrib - Auxiliary detectors for Java programs
 * Copyright (C) 2005-2014 Dave Brosius
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.mebigfatguy.fbcontrib.collect;

import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.CustomUserValue;
import edu.umd.cs.findbugs.ba.ClassContext;

/**
 * collects methods that return a collection that could be created thru an immutable method 
 * such as Arrays.aslist, etc.
 */
@CustomUserValue
public class CollectMethodsReturningImmutableCollections extends BytecodeScanningDetector {

    private static Set<String> IMMUTABLE_PRODUCING_METHODS = new HashSet<String>();
    static {
        IMMUTABLE_PRODUCING_METHODS.add("com/google/common/Collect/Maps.immutableEnumMap");
        IMMUTABLE_PRODUCING_METHODS.add("com/google/common/Collect/Maps.unmodifiableMap");
        IMMUTABLE_PRODUCING_METHODS.add("com/google/common/Collect/Sets.immutableEnumSet");
        IMMUTABLE_PRODUCING_METHODS.add("com/google/common/Collect/Sets.immutableCopy");
        IMMUTABLE_PRODUCING_METHODS.add("java/util/Arrays.asList");
        IMMUTABLE_PRODUCING_METHODS.add("java/util/Collections.unmodifiableCollection");
        IMMUTABLE_PRODUCING_METHODS.add("java/util/Collections.unmodifiableSet");
        IMMUTABLE_PRODUCING_METHODS.add("java/util/Collections.unmodifiableSortedSet");
        IMMUTABLE_PRODUCING_METHODS.add("java/util/Collections.unmodifiableMap");
        IMMUTABLE_PRODUCING_METHODS.add("java/util/Collections.unmodifiableList");
    }
    
    private OpcodeStack stack;
    private String clsName;
    private ImmutabilityType imType;
    
    /**
     * constructs a CMRIC detector given the reporter to report bugs on
     * 
     * @param bugReporter
     *            the sync of bug reports
     */
    public CollectMethodsReturningImmutableCollections(BugReporter bugReporter) {
    }
    
    public void visitClassContext(ClassContext context) {
        try {
            stack = new OpcodeStack();
            clsName = context.getJavaClass().getClassName();
            super.visitClassContext(context);
        } finally {
            stack = null;
        }
    }
    
    public void visitCode(Code obj) {
        stack.resetForMethodEntry(this);
        imType = ImmutabilityType.UNKNOWN;
        super.visitCode(obj);
        
        if ((imType == ImmutabilityType.IMMUTABLE) || (imType == ImmutabilityType.POSSIBLY_IMMUTABLE)) {
            Statistics.getStatistics().addImmutabilityStatus(clsName, getMethod().getName(), getMethod().getSignature(), imType);
        }
    }
    
    public void sawOpcode(int seen) {
        ImmutabilityType seenImmutable = null;
        try {
            stack.precomputation(this);
            
            switch (seen) {
                case INVOKESTATIC: {
                    String className = getClassConstantOperand();
                    String methodName = getNameConstantOperand();
                    
                    if (IMMUTABLE_PRODUCING_METHODS.contains(className + '.' + methodName)) {
                        seenImmutable = ImmutabilityType.IMMUTABLE;
                    }
                }
                //$FALL-THROUGH$   
                case INVOKEINTERFACE:
                case INVOKESPECIAL:
                case INVOKEVIRTUAL: {
                    String className = getClassConstantOperand();
                    String methodName = getNameConstantOperand();
                    String signature = getSigConstantOperand();
                    
                    MethodInfo mi = Statistics.getStatistics().getMethodStatistics(className, methodName, signature);
                    seenImmutable = mi.getImmutabilityType();
                    if (seenImmutable == ImmutabilityType.UNKNOWN)
                        seenImmutable = null;
                }
                break;
                    
                case ARETURN: {
                    if (stack.getStackDepth() > 0) {
                        OpcodeStack.Item item = stack.getStackItem(0);
                        
                        switch (imType) {
                            case UNKNOWN:
                                switch ((ImmutabilityType) item.getUserValue()) {
                                    case IMMUTABLE:
                                        imType = ImmutabilityType.IMMUTABLE;
                                    break;
                                    case POSSIBLY_IMMUTABLE:
                                        imType = ImmutabilityType.POSSIBLY_IMMUTABLE;
                                    break;
                                    default:
                                        imType = ImmutabilityType.MUTABLE;
                                    break;
                                }
                                
                            case IMMUTABLE:
                                if (item.getUserValue() == null) {
                                    imType = ImmutabilityType.POSSIBLY_IMMUTABLE;
                                }
                                break;
                                
                            case POSSIBLY_IMMUTABLE:
                                break;
                                
                            case MUTABLE:
                                if (item.getUserValue() != null) {
                                    imType = ImmutabilityType.POSSIBLY_IMMUTABLE;
                                }
                                break;
                        }
                    }
                    break;
                }
            }
            
        } finally {
            stack.sawOpcode(this,  seen);
            if (seenImmutable != null) {
                if (stack.getStackDepth() > 0) {
                    OpcodeStack.Item item = stack.getStackItem(0);
                    item.setUserValue(seenImmutable);
                }
            }
        }
    }
}