package rip.ysm.gpu;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.Identifier;
/** Legacy native skinning remains disabled; rendering uses vanilla submit collectors. */
public final class GpuRenderPath {
 private GpuRenderPath() {}
 public static boolean tryRender(GeoModel model, PoseStack.Pose pose, float[] boneParams,
   float[] stateBuffer, int renderPartMask, int packedLight, int packedOverlay,
   float r, float g, float b, float a, Identifier textureLocation) { return false; }
 public static void disposeMesh(GeoModel model) { model.gpuMeshHandle=0; }
}
