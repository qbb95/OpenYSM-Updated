import java.util.*;
import java.lang.reflect.*;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import rip.ysm.render.DeferredBufferSource;
import com.elfmcys.yesstevemodel.client.renderer.RenderContext;

public class RenderSmoke {
 static void require(boolean value,String why) { if(!value) throw new AssertionError(why); }
 static void fails(Runnable task,Class<? extends Throwable> expected) {
  try {task.run();throw new AssertionError("expected "+expected.getName());}
  catch(Throwable e){if(!expected.isInstance(e)) throw new AssertionError("wrong exception",e);}
 }
 record Batch(Matrix4f matrix, RenderType type, SubmitNodeCollector.CustomGeometryRenderer renderer) {}
 static class Collector implements InvocationHandler {
  final List<Batch> batches=new ArrayList<>();
  boolean failOnSubmit;
  final SubmitNodeCollector proxy=(SubmitNodeCollector)Proxy.newProxyInstance(RenderSmoke.class.getClassLoader(),new Class<?>[]{SubmitNodeCollector.class},this);
  public Object invoke(Object p,Method m,Object[] a) {
   if(m.getName().equals("submitCustomGeometry")) {
    if(failOnSubmit)throw new IllegalStateException("simulated collector failure");
    PoseStack pose=(PoseStack)a[0];
    batches.add(new Batch(new Matrix4f(pose.last().pose()),(RenderType)a[1],(SubmitNodeCollector.CustomGeometryRenderer)a[2]));return null;
   }
   if(m.getName().equals("order"))return proxy;
   if(m.getName().equals("toString"))return "CapturingSubmitNodeCollector";
   if(m.getName().equals("hashCode"))return System.identityHashCode(p);
   if(m.getName().equals("equals"))return p==a[0];
   throw new AssertionError("unexpected collector method "+m.getName());
  }
 }
 static class Vertex {
  float x,y,z,u,v,u3,v3,nx,ny,nz,lineWidth;
  int color,overlay,light;
 }
 static class Output implements VertexConsumer {
  final List<Vertex> vertices=new ArrayList<>(); Vertex current;
  public VertexConsumer addVertex(float x,float y,float z){current=new Vertex();current.x=x;current.y=y;current.z=z;vertices.add(current);return this;}
  public VertexConsumer setColor(int r,int g,int b,int a){current.color=(a&255)<<24|(r&255)<<16|(g&255)<<8|b&255;return this;}
  public VertexConsumer setColor(int color){current.color=color;return this;}
  public VertexConsumer setUv(float u,float v){current.u=u;current.v=v;return this;}
  public VertexConsumer setUv1(int u,int v){current.overlay=u&65535|v<<16;return this;}
  public VertexConsumer setUv2(int u,int v){current.light=u&65535|v<<16;return this;}
  public VertexConsumer setUv3(float u,float v){current.u3=u;current.v3=v;return this;}
  public VertexConsumer setNormal(float x,float y,float z){current.nx=x;current.ny=y;current.nz=z;return this;}
  public VertexConsumer setLineWidth(float width){current.lineWidth=width;return this;}
 }
 static void quad(VertexConsumer out,PoseStack pose) {
  for(int i=0;i<4;i++)out.addVertex(pose.last(),i,2,3).setColor(17,34,51,68).setUv(i*.25f,.75f).setOverlay(0x12345678).setLight(0x76543210).setUv3(.125f,.625f).setNormal(0,1,0).setLineWidth(2.5f);
 }
 static Output replay(Batch batch) {
  require(batch.matrix().equals(new Matrix4f(),0f),"submitted matrix must be identity");
  var output=new Output();batch.renderer().render(new PoseStack().last(),output);return output;
 }
 public static void main(String[] args) throws Exception {
  RenderType type=RenderTypes.entityCutout(Identifier.fromNamespaceAndPath("yes_steve_model","smoke.png"));
  RenderType other=RenderTypes.entitySolid(Identifier.fromNamespaceAndPath("yes_steve_model","smoke2.png"));
  var checks=new LinkedHashMap<String,Boolean>();
  var captured=new Collector();var buffers=new DeferredBufferSource(captured.proxy);
  fails(()->new DeferredBufferSource(null),NullPointerException.class);
  fails(()->buffers.getBuffer(null),NullPointerException.class);
  checks.put("null_inputs_rejected",true);
  buffers.endBatch();require(captured.batches.isEmpty(),"empty batch submitted geometry");
  PoseStack pose=new PoseStack();pose.translate(10,20,30);pose.scale(2,3,4);
  VertexConsumer stale=buffers.getBuffer(type);quad(stale,pose);
  buffers.endBatch(type);require(captured.batches.size()==1,"first batch count");
  stale.addVertex(999,999,999).setColor(0).setUv(-99,-99);pose.translate(1000,1000,1000);
  Output first=replay(captured.batches.getFirst());require(first.vertices.size()==4,"snapshot changed vertex count");
  for(int i=0;i<4;i++) {
   Vertex v=first.vertices.get(i);
   require(v.x==10+i*2 && v.y==26 && v.z==42,"transformation applied incorrectly/doubled");
   require(v.color==0x44112233 && v.u==i*.25f && v.v==.75f,"color/uv snapshot changed");
   require(v.overlay==0x12345678 && v.light==0x76543210,"packed light/overlay changed");
   require(v.u3==.125f && v.v3==.625f,"26.3 UV3 attributes changed");
   require(v.nx==0 && v.ny==1 && v.nz==0 && v.lineWidth==2.5f,"normal/line width changed");
  }
  checks.put("snapshots_immutable_after_source_mutation",true);
  checks.put("transforms_applied_once",true);checks.put("all_vertex_attributes_preserved",true);
  buffers.endBatch(type);buffers.endBatch();require(captured.batches.size()==1,"duplicate flush");
  checks.put("repeated_flush_does_not_duplicate",true);
  quad(buffers.getBuffer(type),new PoseStack());quad(buffers.getBuffer(other),new PoseStack());
  buffers.endBatch(type);require(captured.batches.size()==2,"typed flush not selective");
  buffers.initialize();require(captured.batches.size()==3 && captured.batches.get(2).type()==other,"initialize failed to flush remaining type");
  checks.put("typed_flush_and_initialize_flush",true);
  fails(RenderContext::bufferSource,IllegalStateException.class);fails(RenderContext::exit,IllegalStateException.class);
  require(RenderContext.collector()==null && RenderContext.camera()==null,"scope leaked before enter");
  checks.put("out_of_scope_access_rejected",true);
  var outer=new Collector();var inner=new Collector();
  RenderContext.enter(outer.proxy,null);var outerBuffers=RenderContext.bufferSource();quad(outerBuffers.getBuffer(type),new PoseStack());
  RenderContext.enter(inner.proxy,null);require(RenderContext.collector()==inner.proxy,"inner collector wrong");quad(RenderContext.bufferSource().getBuffer(type),new PoseStack());
  RenderContext.exit();require(inner.batches.size()==1 && outer.batches.isEmpty(),"nested flush leaked into parent");
  require(RenderContext.collector()==outer.proxy && RenderContext.bufferSource()==outerBuffers,"outer scope not restored");
  RenderContext.exit();require(outer.batches.size()==1 && RenderContext.collector()==null,"outer flush/cleanup failed");
  require(replay(inner.batches.getFirst()).vertices.size()==4 && replay(outer.batches.getFirst()).vertices.size()==4,"scope snapshots wrong");
  checks.put("nested_scopes_restore_and_flush_independently",true);
  var workerError=new java.util.concurrent.atomic.AtomicReference<Throwable>();
  Thread thread=new Thread(()->{require(RenderContext.collector()==null,"context not thread local");RenderContext.enter(inner.proxy,null);RenderContext.exit();});
  thread.setUncaughtExceptionHandler((t,e)->workerError.set(e));thread.start();thread.join();
  if(workerError.get()!=null)throw new AssertionError("worker failed",workerError.get());
  require(RenderContext.collector()==null,"worker scope leaked");checks.put("thread_scopes_isolated",true);
  var failing=new Collector();failing.failOnSubmit=true;
  RenderContext.enter(outer.proxy,null);var saved=RenderContext.bufferSource();
  RenderContext.enter(failing.proxy,null);quad(RenderContext.bufferSource().getBuffer(type),new PoseStack());
  fails(RenderContext::exit,IllegalStateException.class);
  require(RenderContext.collector()==outer.proxy && RenderContext.bufferSource()==saved,"collector failure broke outer scope");
  RenderContext.exit();require(RenderContext.collector()==null,"scope leaked after collector error");
  checks.put("collector_failure_restores_parent_scope",true);
  System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("status","PASS","real_minecraft","26.3","real_render_types",true,"checks",checks,"limits",List.of("Collector and output VertexConsumer are test doubles; no GPU upload or in-game display is exercised."))));
 }
}
