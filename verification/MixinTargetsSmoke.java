import com.google.gson.*;
import java.io.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Check string-bound mixin methods and fields against the selected game JAR. */
public class MixinTargetsSmoke {
    static final Map<String, ClassNode> classes = new HashMap<>();
    static int bindings;
    static final List<String> failures=new ArrayList<>();
    static ClassNode read(String name) throws IOException {
        if (classes.containsKey(name)) return classes.get(name);
        try (var input = MixinTargetsSmoke.class.getClassLoader().getResourceAsStream(name + ".class")) {
            if (input == null) throw new AssertionError("Missing class: " + name);
            var node = new ClassNode(); new ClassReader(input).accept(node, 0);
            classes.put(name,node); return node;
        }
    }
    static List<AnnotationNode> annotations(List<AnnotationNode> a,List<AnnotationNode> b) {
        var result=new ArrayList<AnnotationNode>(); if(a!=null)result.addAll(a);if(b!=null)result.addAll(b);return result;
    }
    static Object value(AnnotationNode annotation,String key) {
        if(annotation.values==null)return null;
        for(int i=0;i<annotation.values.size();i+=2)if(annotation.values.get(i).equals(key))return annotation.values.get(i+1);
        return null;
    }
    static boolean field(ClassNode target,String name) throws IOException {
        for(var f:target.fields)if(f.name.equals(name))return true;
        return target.superName!=null && field(read(target.superName),name);
    }
    static void method(ClassNode target,String selector,String origin) {
        int descriptor=selector.indexOf('(');String name=descriptor<0?selector:selector.substring(0,descriptor);
        String desc=descriptor<0?null:selector.substring(descriptor);
        if(target.methods.stream().noneMatch(m->m.name.equals(name)&&(desc==null||m.desc.equals(desc))))
            failures.add(origin+" -> missing "+target.name+"."+selector);
        bindings++;
    }
    static void inspect(String name) throws Exception {
        var mixin=read(name);var targets=new ArrayList<String>();
        for(var a:annotations(mixin.visibleAnnotations,mixin.invisibleAnnotations))if(a.desc.endsWith("/Mixin;")) {
            if(value(a,"value") instanceof List<?> types)for(var type:types)targets.add(((Type)type).getInternalName());
            if(value(a,"targets") instanceof List<?> strings)for(var type:strings)targets.add(type.toString().replace('.','/'));
        }
        for(String targetName:targets) {
            var target=read(targetName);
            for(var m:mixin.methods)for(var a:annotations(m.visibleAnnotations,m.invisibleAnnotations)) {
                if(value(a,"method") instanceof List<?> selectors)
                    for(var selector:selectors)method(target,selector.toString(),name+"."+m.name);
                if(a.desc.endsWith("/Accessor;")) {
                    String selected=(String)value(a,"value");
                    if(selected==null||!field(target,selected))failures.add(name+" -> missing field "+selected);
                    bindings++;
                }
            }
        }
    }
    public static void main(String[] args) throws Exception {
        for(String resource:List.of("yes_steve_model.mixins.json","yes_steve_model_fabric.mixins.json")) {
            try(var stream=MixinTargetsSmoke.class.getClassLoader().getResourceAsStream(resource)) {
                if(stream==null)throw new AssertionError("Missing mixin config "+resource);
                var config=JsonParser.parseReader(new InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                for(String group:List.of("mixins","client","server"))if(config.has(group))
                    for(var mixin:config.getAsJsonArray(group))inspect((config.get("package").getAsString()+"."+mixin.getAsString()).replace('.','/'));
            }
        }
        if(!failures.isEmpty())throw new AssertionError(String.join("\n",failures));
        System.out.println("PASS MixinTargetsSmoke: "+bindings+" method/field bindings; live launch still required for injection/callback validation");
    }
}
