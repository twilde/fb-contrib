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
package com.mebigfatguy.fbcontrib.detect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XFactory;

public class PoorMansEnum extends BytecodeScanningDetector {

    private BugReporter bugReporter;
    private Map<String, Set<Object>> fieldValues;
    private Map<String, Field> nameToField;
    private OpcodeStack stack;
    
    public PoorMansEnum(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }
    
    @Override
    public void visitClassContext(ClassContext classContext) {
        try {
            fieldValues = new HashMap<String, Set<Object>>();
            nameToField = new HashMap<String, Field>();
            JavaClass cls = classContext.getJavaClass();
            for (Field f : cls.getFields()) {
                if (f.isPrivate() && !f.isSynthetic()) {
                    fieldValues.put(f.getName(), null);
                    nameToField.put(f.getName(), f);
                }
            }
            if (!fieldValues.isEmpty()) {
                stack = new OpcodeStack();
                
                super.visitClassContext(classContext);
                
                for (Map.Entry<String, Set<Object>> fieldInfo : fieldValues.entrySet()) {
                    Set<Object> values = fieldInfo.getValue();
                    if (values != null) {
                        if (values.size() >= 3) {
                            bugReporter.reportBug(new BugInstance(this, "PME_POOR_MANS_ENUM", NORMAL_PRIORITY)
                                        .addClass(this)
                                        .addField(XFactory.createXField(cls, nameToField.get(fieldInfo.getKey()))));
                        }
                    }
                }
            }
        } finally {
            fieldValues = null;
            stack = null;
        }
    }
    
    @Override
    public void visitCode(Code obj) {
        if (!fieldValues.isEmpty()) {
            stack.resetForMethodEntry(this);
            super.visitCode(obj);
        }
    }
    
    @Override
    public void sawOpcode(int seen) {
        try {
            if (fieldValues.isEmpty()) {
                return;
            }
            
            if (seen == PUTFIELD) {
                String fieldName = getNameConstantOperand();
                if (fieldValues.containsKey(fieldName)) {
                    if (stack.getStackDepth() > 0) {
                        OpcodeStack.Item item = stack.getStackItem(0);
                        Object cons = item.getConstant();
                        if (cons == null) {
                            fieldValues.remove(fieldName);
                            nameToField.remove(fieldName);
                        } else {
                            Set<Object> values = fieldValues.get(fieldName);
                            if (values == null) {
                                values = new HashSet<Object>();
                                fieldValues.put(fieldName, values);
                            }
                            values.add(cons);
                        } 
                    }
                }
            }
        } finally {
            stack.sawOpcode(this, seen);
        }
    }
}
