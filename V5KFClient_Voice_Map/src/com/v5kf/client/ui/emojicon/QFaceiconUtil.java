package com.v5kf.client.ui.emojicon;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.text.SpannableString;
import android.widget.TextView;

public class QFaceiconUtil {
	
	public static Map<String, String> faceMap = new LinkedHashMap<String, String>();
	
	static {
		faceMap.put("/::)", "qf000");
		faceMap.put("/::~", "qf001");
		faceMap.put("/::B", "qf002");
		faceMap.put("/::|", "qf003");
		faceMap.put("/:8-)", "qf004");
		faceMap.put("/::<", "qf005");
		faceMap.put("/::$", "qf006"); //
		faceMap.put("/::X", "qf007");
		faceMap.put("/::Z", "qf008");
		faceMap.put("/::'(", "qf009");
		faceMap.put("/::-|", "qf010");
		faceMap.put("/::@", "qf011");
		faceMap.put("/::P", "qf012");
		faceMap.put("/::D", "qf013");
		faceMap.put("/::O", "qf014");
		faceMap.put("/::(", "qf015");
		faceMap.put("/::+", "qf016");
		faceMap.put("/:--b", "qf017");
		faceMap.put("/::Q", "qf018");
		faceMap.put("/::T", "qf019");
		faceMap.put("/:,@P", "qf020");
		faceMap.put("/:,@-D", "qf021");
		faceMap.put("/::d", "qf022");
		faceMap.put("/:,@o", "qf023");
		faceMap.put("/::g", "qf024");
		faceMap.put("/:|-)", "qf025");
		faceMap.put("/::!", "qf026");
		faceMap.put("/::L", "qf027");
		faceMap.put("/::>", "qf028");
		faceMap.put("/::,@", "qf029");
		faceMap.put("/:,@f", "qf030");
		faceMap.put("/::-S", "qf031");
		faceMap.put("/:?", "qf032");
		faceMap.put("/:,@x", "qf033");
		faceMap.put("/:,@@", "qf034");
		faceMap.put("/::8", "qf035");
		faceMap.put("/:,@!", "qf036");
		faceMap.put("/:!!!", "qf037");
		faceMap.put("/:xx", "qf038");
		faceMap.put("/:bye", "qf039");
		faceMap.put("/:wipe", "qf040");
		faceMap.put("/:dig", "qf041");
		faceMap.put("/:handclap", "qf042");
		faceMap.put("/:&-(", "qf043");
		faceMap.put("/:B-)", "qf044");
		faceMap.put("/:<@", "qf045");
		faceMap.put("/:@>", "qf046");
		faceMap.put("/::-O", "qf047");
		faceMap.put("/:>-|", "qf048");
		faceMap.put("/:P-(", "qf049");
		faceMap.put("/::'|", "qf050");
		faceMap.put("/:X-)", "qf051");
		faceMap.put("/::*", "qf052");
		faceMap.put("/:@x", "qf053");
		faceMap.put("/:8*", "qf054");
		faceMap.put("/:pd", "qf055");
		faceMap.put("/:<W>", "qf056");
		faceMap.put("/:beer", "qf057");
		faceMap.put("/:basketb", "qf058");
		faceMap.put("/:oo", "qf059");
		faceMap.put("/:coffee", "qf060");
		faceMap.put("/:eat", "qf061");
		faceMap.put("/:pig", "qf062");
		faceMap.put("/:rose", "qf063");
		faceMap.put("/:fade", "qf064");
		faceMap.put("/:showlove", "qf065");
		faceMap.put("/:heart", "qf066");
		faceMap.put("/:break", "qf067");
		faceMap.put("/:cake", "qf068");
		faceMap.put("/:li", "qf069");
		faceMap.put("/:bome", "qf070");
		faceMap.put("/:kn", "qf071");
		faceMap.put("/:footb", "qf072");
		faceMap.put("/:ladybug", "qf073");
		faceMap.put("/:shit", "qf074");
		faceMap.put("/:moon", "qf075");
		faceMap.put("/:sun", "qf076");
		faceMap.put("/:gift", "qf077");
		faceMap.put("/:hug", "qf078");
		faceMap.put("/:strong", "qf079");
		faceMap.put("/:weak", "qf080");
		faceMap.put("/:share", "qf081");
		faceMap.put("/:v", "qf082");
		faceMap.put("/:@)", "qf083");
		faceMap.put("/:jj", "qf084");
		faceMap.put("/:@@", "qf085");
		faceMap.put("/:bad", "qf086");
		faceMap.put("/:lvu", "qf087");
		faceMap.put("/:no", "qf088");
		faceMap.put("/:ok", "qf089");
		faceMap.put("/:love", "qf090");
		faceMap.put("/:<L>", "qf091");
		faceMap.put("/:jump", "qf092");
		faceMap.put("/:shake", "qf093");
		faceMap.put("/:<O>", "qf094");
		faceMap.put("/:circle", "qf095");
		faceMap.put("/:kotow", "qf096");
		faceMap.put("/:turn", "qf097");
		faceMap.put("/:skip", "qf098");
		faceMap.put("/:&>", "qf099");
		faceMap.put("/:#-0", "qf100");
		faceMap.put("/:hiphot", "qf101");
		faceMap.put("/:kiss", "qf102");
		faceMap.put("/:<&", "qf103");
		faceMap.put("/:oY", "qf104");
	};

	public static void showQFaceText(TextView tv, CharSequence str, int size) {		
		// 判断QQ表情的正则表达式 
	    String qqfaceRegex = "/::\\)|/::~|/::B|/::\\||/:8-\\)|/::<|/::\\$|/::X|/::Z|/::" +
	    		"'\\(|/::-\\||/::@|/::P|/::D|/::O|/::\\(|/::\\+|/:--b|/::Q|/::T|/:,@P" +
	    		"|/:,@-D|/::d|/:,@o|/::g|/:\\|-\\)|/::!|/::L|/::>|/::,@|/:,@f|/::-S|/" +
	    		":\\?|/:,@x|/:,@@|/::8|/:,@!|/:!!!|/:xx|/:bye|/:wipe|/:dig|/:handclap" +
	    		"|/:&-\\(|/:B-\\)|/:<@|/:@>|/::-O|/:>-\\||/:P-\\(|/::'\\||/:X-\\)|/::" +
	    		"\\*|/:@x|/:8\\*|/:pd|/:<W>|/:beer|/:basketb|/:oo|/:coffee|/:eat|/:pi" +
	    		"g|/:rose|/:fade|/:showlove|/:heart|/:break|/:cake|/:li|/:bome|/:kn|/" +
	    		":footb|/:ladybug|/:shit|/:moon|/:sun|/:gift|/:hug|/:strong|/:weak|/:" +
	    		"share|/:v|/:@\\)|/:jj|/:@@|/:bad|/:lvu|/:no|/:ok|/:love|/:<L>|/:jump" +
	    		"|/:shake|/:<O>|/:circle|/:kotow|/:turn|/:skip|/:oY|/:#-0|/:hiphot|/:" +
	    		"kiss|/:<&|/:&>";
	    
		try {
			SpannableString spannableString = QFaceExpressionUtil
					.getExpressionString(tv.getContext(), str, qqfaceRegex, size);
			tv.setText(spannableString);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}
	
	public static SpannableString getQFaceText(Context context, CharSequence str, int size) {
		if (null == str || str.length() == 0) {
			return null;
		}
		
		// 判断QQ表情的正则表达式 
	    String qqfaceRegex = "/::\\)|/::~|/::B|/::\\||/:8-\\)|/::<|/::\\$|/::X|/::Z|/::" +
	    		"'\\(|/::-\\||/::@|/::P|/::D|/::O|/::\\(|/::\\+|/:--b|/::Q|/::T|/:,@P" +
	    		"|/:,@-D|/::d|/:,@o|/::g|/:\\|-\\)|/::!|/::L|/::>|/::,@|/:,@f|/::-S|/" +
	    		":\\?|/:,@x|/:,@@|/::8|/:,@!|/:!!!|/:xx|/:bye|/:wipe|/:dig|/:handclap" +
	    		"|/:&-\\(|/:B-\\)|/:<@|/:@>|/::-O|/:>-\\||/:P-\\(|/::'\\||/:X-\\)|/::" +
	    		"\\*|/:@x|/:8\\*|/:pd|/:<W>|/:beer|/:basketb|/:oo|/:coffee|/:eat|/:pi" +
	    		"g|/:rose|/:fade|/:showlove|/:heart|/:break|/:cake|/:li|/:bome|/:kn|/" +
	    		":footb|/:ladybug|/:shit|/:moon|/:sun|/:gift|/:hug|/:strong|/:weak|/:" +
	    		"share|/:v|/:@\\)|/:jj|/:@@|/:bad|/:lvu|/:no|/:ok|/:love|/:<L>|/:jump" +
	    		"|/:shake|/:<O>|/:circle|/:kotow|/:turn|/:skip|/:oY|/:#-0|/:hiphot|/:" +
	    		"kiss|/:<&|/:&>";
	    SpannableString spannableString = null;
		try {
			spannableString = QFaceExpressionUtil.getExpressionString(context, str, qqfaceRegex, size);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		
		return spannableString;
	}
	
	/** 
	 * 判断是否是QQ表情 
	 *  
	 * @param content 
	 * @return 
	 */ 
	public static boolean isQqFace(CharSequence content) { 
	    boolean result = false; 
	 
	    // 判断QQ表情的正则表达式 
	    String qqfaceRegex = "/::\\)|/::~|/::B|/::\\||/:8-\\)|/::<|/::\\$|/::X|/::Z|/::" +
	    		"'\\(|/::-\\||/::@|/::P|/::D|/::O|/::\\(|/::\\+|/:--b|/::Q|/::T|/:,@P" +
	    		"|/:,@-D|/::d|/:,@o|/::g|/:\\|-\\)|/::!|/::L|/::>|/::,@|/:,@f|/::-S|/" +
	    		":\\?|/:,@x|/:,@@|/::8|/:,@!|/:!!!|/:xx|/:bye|/:wipe|/:dig|/:handclap" +
	    		"|/:&-\\(|/:B-\\)|/:<@|/:@>|/::-O|/:>-\\||/:P-\\(|/::'\\||/:X-\\)|/::" +
	    		"\\*|/:@x|/:8\\*|/:pd|/:<W>|/:beer|/:basketb|/:oo|/:coffee|/:eat|/:pi" +
	    		"g|/:rose|/:fade|/:showlove|/:heart|/:break|/:cake|/:li|/:bome|/:kn|/" +
	    		":footb|/:ladybug|/:shit|/:moon|/:sun|/:gift|/:hug|/:strong|/:weak|/:" +
	    		"share|/:v|/:@\\)|/:jj|/:@@|/:bad|/:lvu|/:no|/:ok|/:love|/:<L>|/:jump" +
	    		"|/:shake|/:<O>|/:circle|/:kotow|/:turn|/:skip|/:oY|/:#-0|/:hiphot|/:" +
	    		"kiss|/:<&|/:&>";
	    Pattern p = Pattern.compile(qqfaceRegex, Pattern.CASE_INSENSITIVE);
	    Matcher m = p.matcher(content); 
	    if (m.find()) { 
	        result = true; 
	    } 
	    return result; 
	}
	
	public static String getQQFaceImgName(String qqFaceKey) {
		
		return faceMap.get(qqFaceKey);
	}
}
