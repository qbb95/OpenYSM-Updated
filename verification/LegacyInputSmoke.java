import com.elfmcys.yesstevemodel.util.LegacyInputCodes;
import com.mojang.blaze3d.platform.InputConstants;

public class LegacyInputSmoke {
 public static void main(String[] args) {
  int[][] keys={{InputConstants.KEY_A,65},{InputConstants.KEY_W,87},{InputConstants.KEY_Y,89},
   {InputConstants.KEY_0,48},{InputConstants.KEY_1,49},{InputConstants.KEY_ESCAPE,256},
   {InputConstants.KEY_LEFT,263},{InputConstants.KEY_RIGHT,262},{InputConstants.KEY_LSHIFT,340},
   {InputConstants.KEY_RSHIFT,344},{InputConstants.KEY_F1,290},{InputConstants.KEY_F12,301},
   {InputConstants.KEY_NUMPAD0,320},{InputConstants.KEY_NUMPADENTER,335}};
  for(int[] pair:keys) if(LegacyInputCodes.keyboard(pair[0])!=pair[1]) throw new AssertionError("Incorrect legacy key "+pair[1]);
  if(LegacyInputCodes.mouse(InputConstants.MOUSE_BUTTON_LEFT)!=0
    || LegacyInputCodes.mouse(InputConstants.MOUSE_BUTTON_RIGHT)!=1
    || LegacyInputCodes.mouse(InputConstants.MOUSE_BUTTON_MIDDLE)!=2
    || LegacyInputCodes.mouse(InputConstants.MOUSE_BUTTON_8)!=7
    || LegacyInputCodes.mouse(-1)!=-1 || LegacyInputCodes.keyboard(-1)!=-1)
   throw new AssertionError("Incorrect mouse/unknown input mapping");
  System.out.println("PASS: SDL inputs preserve legacy model key numbers and left/right/middle buttons");
 }
}
