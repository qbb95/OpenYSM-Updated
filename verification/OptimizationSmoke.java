import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.FloatValue;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.bone.Vector3v;
import com.elfmcys.yesstevemodel.resource.*;
import com.elfmcys.yesstevemodel.resource.pojo.RawYsmModel;
import com.elfmcys.yesstevemodel.model.format.*;
import rip.ysm.legacy.YesModelUtils;
import rip.ysm.security.YsmCrypt;

public class OptimizationSmoke {
 static void require(boolean v, String message) { if(!v) throw new AssertionError(message); }
 public static void main(String[] args) throws Exception {
  float[] values={-0f,0f,Float.NaN,Float.NEGATIVE_INFINITY,Float.POSITIVE_INFINITY,Float.MIN_VALUE,
    Float.MAX_VALUE,-17f,-16f,-1f,1f,16f,17f,0.1f,-0.1f};
  for(float v:values) {
   var a=new FloatValue(v); var b=FloatValue.of(v);
   require(Float.floatToRawIntBits(a.evalAsFloat(null))==Float.floatToRawIntBits(b.evalAsFloat(null)), "float bits: "+v);
   require(a.evalAsInt(null)==b.evalAsInt(null) && a.evalAsBoolean(null)==b.evalAsBoolean(null), "conversions");
   require(a.evalSafe(null).equals(b.evalSafe(null)) && a.evalUnsafe(null).equals(b.evalUnsafe(null)), "boxed evaluation");
  }
  var v1=new Vector3v(FloatValue.of(-2),FloatValue.of(0.5f),FloatValue.of(3));
  var v2=new Vector3v(FloatValue.of(6),FloatValue.of(7),FloatValue.of(8));
  var p1=v1.eval(null); var p2=v2.eval(null);
  require(p1!=p2 && p1.x==-2 && p1.y==0.5f && p1.z==3 && p2.x==6, "independent scratch vectors");
  p1.set(99); require(v1.eval(null)==p1 && p1.x==-2 && p2.x==6,"evaluation restores values without cross-frame aliasing");

  Path parent=Path.of(args[0]); Files.createDirectories(parent);
  Path root=Files.createTempDirectory(parent,"run-");
  Path entries=root.resolve("entries"), binaries=root.resolve("binaries"); Files.createDirectories(binaries);
  byte[] key=new byte[56]; new Random(27).nextBytes(key);
  byte[] source=Files.readAllBytes(Path.of(args[1]));
  RawYsmModel raw;
  try(var reader=new YSMFolderDeserializer(YesModelUtils.input(source))) {raw=reader.deserialize();}
  var info=YSMClientMapper.buildModelInfo(raw);
  Map<String,String[]> animations=new LinkedHashMap<>();
  raw.mainEntity.animationFiles.forEach((name,file)->animations.put(name,file.animations.keySet().toArray(String[]::new)));
  var data=new ServerModelData("original",new ServerAnimationInfo(animations,raw.mainEntity.textures.keySet().toArray(String[]::new)),new Object[0],new Object[0],info,false,false);
  long[] hashes=YsmCrypt.calculateModelHashes(raw.properties.sha256,key);
  String filename=String.format("%016x%016x",hashes[0],hashes[1]);
  byte[] binary=YsmCrypt.encryptServerCache(new byte[]{1,2,3},key,hashes[0],hashes[1]);
  Files.write(binaries.resolve(filename),binary);
  String fingerprint=ServerMetadataCache.fingerprint(source);
  var cache=new ServerMetadataCache(entries,binaries,key); cache.store(fingerprint,raw,data);
  Set<String> valid=new HashSet<>(); var restored=cache.load(fingerprint,"renamed/path.ysm",true,valid);
  require(restored!=null && restored.isAuth() && restored.getModelId().equals("renamed/path.ysm"),"restore identity/current authorization");
  var gson=new Gson();
  require(gson.toJsonTree(info).equals(gson.toJsonTree(restored.getLoadedModelData())),"metadata, properties and statistics round trip");
  require(data.getModelInfo().getAnimations().equals(restored.getModelInfo().getAnimations())
    && data.getModelInfo().getTextures().equals(restored.getModelInfo().getTextures()),"animation/texture names round trip");
  require(valid.contains(filename),"preserve valid binary cache");
  var steve=cache.load(fingerprint,"misc/2_steve",false,new HashSet<>());
  require(steve!=null && steve.isCustomSkinModel() && !steve.isAuth(),"alias-specific skin and authorization flags");
  byte[] changed=source.clone(); changed[changed.length-1]^=1;
  require(cache.load(ServerMetadataCache.fingerprint(changed),"changed",false,new HashSet<>())==null,"source change invalidation");
  byte[] broken=binary.clone(); broken[broken.length/2]^=1; Files.write(binaries.resolve(filename),broken);
  require(cache.load(fingerprint,"broken",false,new HashSet<>())==null,"damaged binary falls back");
  Files.write(binaries.resolve(filename),binary);
  Files.writeString(entries.resolve(fingerprint+".json"),"{bad json");
  require(cache.load(fingerprint,"broken-json",false,new HashSet<>())==null,"damaged metadata falls back");
  Path folder=root.resolve("folder"); Files.createDirectories(folder); Files.writeString(folder.resolve("a"),"before");
  String first=ServerMetadataCache.fingerprint(folder); Files.writeString(folder.resolve("a"),"after");
  require(!first.equals(ServerMetadataCache.fingerprint(folder)),"folder content change invalidation");
  String second=ServerMetadataCache.fingerprint(folder); Files.move(folder.resolve("a"),folder.resolve("b"));
  require(!second.equals(ServerMetadataCache.fingerprint(folder)),"folder rename invalidation");
  System.out.println("PASS: numeric edge cases, independent frame evaluation, metadata equivalence, aliases/authorization, source/binary/JSON/folder invalidation");
 }
}
