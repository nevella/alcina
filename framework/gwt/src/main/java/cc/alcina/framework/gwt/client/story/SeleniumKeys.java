package cc.alcina.framework.gwt.client.story;

import java.util.Arrays;
import java.util.Iterator;
/*
 * Shadow of the Selenium Keys class
 */
public enum SeleniumKeys  implements CharSequence {
   NULL('\ue000'),
   CANCEL('\ue001'),
   HELP('\ue002'),
   BACK_SPACE('\ue003'),
   TAB('\ue004'),
   CLEAR('\ue005'),
   RETURN('\ue006'),
   ENTER('\ue007'),
   SHIFT('\ue008'),
   LEFT_SHIFT(SHIFT),
   CONTROL('\ue009'),
   LEFT_CONTROL(CONTROL),
   ALT('\ue00a'),
   LEFT_ALT(ALT),
   PAUSE('\ue00b'),
   ESCAPE('\ue00c'),
   SPACE('\ue00d'),
   PAGE_UP('\ue00e'),
   PAGE_DOWN('\ue00f'),
   END('\ue010'),
   HOME('\ue011'),
   LEFT('\ue012'),
   ARROW_LEFT(LEFT),
   UP('\ue013'),
   ARROW_UP(UP),
   RIGHT('\ue014'),
   ARROW_RIGHT(RIGHT),
   DOWN('\ue015'),
   ARROW_DOWN(DOWN),
   INSERT('\ue016'),
   DELETE('\ue017'),
   SEMICOLON('\ue018'),
   EQUALS('\ue019'),
   NUMPAD0('\ue01a'),
   NUMPAD1('\ue01b'),
   NUMPAD2('\ue01c'),
   NUMPAD3('\ue01d'),
   NUMPAD4('\ue01e'),
   NUMPAD5('\ue01f'),
   NUMPAD6('\ue020'),
   NUMPAD7('\ue021'),
   NUMPAD8('\ue022'),
   NUMPAD9('\ue023'),
   MULTIPLY('\ue024'),
   ADD('\ue025'),
   SEPARATOR('\ue026'),
   SUBTRACT('\ue027'),
   DECIMAL('\ue028'),
   DIVIDE('\ue029'),
   F1('\ue031'),
   F2('\ue032'),
   F3('\ue033'),
   F4('\ue034'),
   F5('\ue035'),
   F6('\ue036'),
   F7('\ue037'),
   F8('\ue038'),
   F9('\ue039'),
   F10('\ue03a'),
   F11('\ue03b'),
   F12('\ue03c'),
   META('\ue03d'),
   COMMAND(META),
   ZENKAKU_HANKAKU('\ue040');

   private final char keyCode;
   private final int codePoint;

   private SeleniumKeys(SeleniumKeys key) {
      this(key.charAt(0));
   }

   private SeleniumKeys(char keyCode) {
      this.keyCode = keyCode;
	  //don't care...much
      this.codePoint = (int) keyCode;
   }

   public int getCodePoint() {
      return this.codePoint;
   }

   public char charAt(int index) {
      return index == 0 ? this.keyCode : '\u0000';
   }

   public int length() {
      return 1;
   }

   public CharSequence subSequence(int start, int end) {
      if (start == 0 && end == 1) {
         return String.valueOf(this.keyCode);
      } else {
         throw new IndexOutOfBoundsException();
      }
   }

   public String toString() {
      return String.valueOf(this.keyCode);
   }

   public static String chord(CharSequence... value) {
      return chord((Iterable)Arrays.asList(value));
   }

   public static String chord(Iterable<CharSequence> value) {
      StringBuilder builder = new StringBuilder();
      Iterator var2 = value.iterator();

      while(var2.hasNext()) {
         CharSequence seq = (CharSequence)var2.next();
         builder.append(seq);
      }

      builder.append(NULL);
      return builder.toString();
   }

   public static SeleniumKeys getKeyFromUnicode(char key) {
	SeleniumKeys[] var1 = values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
		SeleniumKeys unicodeKey = var1[var3];
         if (unicodeKey.charAt(0) == key) {
            return unicodeKey;
         }
      }

      return null;
   }
}
