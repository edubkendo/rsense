package org.cx4a.rsense.ruby;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import org.cx4a.rsense.util.Logger;

public class RubyModule extends RubyObject {
    private String baseName;
    private RubyModule parent;
    private Map<String, DynamicMethod> methods;
    private Map<String, IRubyObject> constants;
    private List<RubyModule> includes;

    public static RubyModule newModule(Ruby runtime, String baseName, RubyModule parent) {
        RubyModule module = new RubyModule(runtime, parent);
        module.setBaseName(baseName);
        module.makeMetaClass(runtime.getModule());
        return module;
    }

    protected RubyModule(Ruby runtime) {
        this(runtime, null);
    }
    
    protected RubyModule(Ruby runtime, RubyModule parent) {
        this(runtime, runtime.getModule(), parent);
    }
    
    protected RubyModule(Ruby runtime, RubyClass metaClass, RubyModule parent) {
        super(runtime, metaClass);
        this.parent = parent;
        this.methods = new HashMap<String, DynamicMethod>();
        this.constants = new HashMap<String, IRubyObject>();
        this.includes = new ArrayList<RubyModule>();
    }

    public String getBaseName() {
        return baseName;
    }

    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    public RubyModule getParent() {
        return parent;
    }

    public void setConstant(String name, IRubyObject constant) {
        constants.put(name, constant);
    }

    public IRubyObject getConstant(String name) {
        IRubyObject constant = constants.get(name);
        if (constant == null && parent != null) {
            return parent.getConstant(name);
        }
        return constant;
    }

    public boolean isConstantDefined(String name) {
        return constants.containsKey(name);
    }

    public RubyClass defineOrGetClassUnder(String name, RubyClass superClass) {
        if (isConstantDefined(name)) {
            IRubyObject object = getConstant(name);
            if (object instanceof RubyClass) {
                return (RubyClass) object;
            } else {
                Logger.error("%s is not class", name);
                return null;
            }
        } else {
            RubyClass klass = RubyClass.newClass(getRuntime(), name, superClass, getRuntime().getContext().getFrameModule());
            setConstant(name, klass);
            return klass;
        }
    }

    public RubyModule defineOrGetModuleUnder(String name) {
        if (isConstantDefined(name)) {
            IRubyObject object = getConstant(name);
            if (object.getClass() == RubyModule.class) {
                return (RubyModule) object;
            } else {
                Logger.error("%s is not module", name);
                return null;
            }
        } else {
            RubyModule module = RubyModule.newModule(getRuntime(), name, getRuntime().getContext().getFrameModule());
            setConstant(name, module);
            return module;
        }
    }

    public void addMethod(String name, DynamicMethod method) {
        methods.put(name, method);
    }

    public DynamicMethod getMethod(String name) {
        return methods.get(name);
    }

    public Set<String> getMethods(boolean inheritedToo) {
        Set<String> result = new HashSet<String>(methods.keySet());
        if (inheritedToo) {
            for (RubyModule module : includes) {
                result.addAll(module.getPublicMethods(inheritedToo));
            }
        }
        return result;
    }

    public Set<String> getPublicMethods(boolean inheritedToo) {
        Set<String> result = getVisibleMethods(Visibility.PUBLIC);
        if (inheritedToo) {
            for (RubyModule module : includes) {
                result.addAll(module.getPublicMethods(inheritedToo));
            }
        }
        return result;
    }

    private Set<String> getVisibleMethods(Visibility visibility) {
        Set<String> result = new HashSet<String>();
        for (Map.Entry<String, DynamicMethod> entry : methods.entrySet()) {
            if (entry.getValue().getVisibility() == visibility) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public DynamicMethod searchMethod(String name) {
        DynamicMethod method = getMethod(name);
        if (method == null) {
            for (RubyModule module : includes) {
                if ((method = module.searchMethod(name)) != null) {
                    break;
                }
            }
        }
        return method;
    }

    public void includeModule(RubyModule module) {
        includes.add(module);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (parent != null && parent != getRuntime().getObject()) {
            sb.append(parent.toString());
            sb.append("::");
        }
        sb.append(baseName != null ? baseName : super.toString());
        return sb.toString();
    }
}
