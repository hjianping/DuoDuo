package org.qiunet.utils.string;

import org.apache.commons.lang3.StringUtils;
import org.qiunet.utils.math.MathUtil;
import org.slf4j.helpers.MessageFormatter;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * 字符处理相关的工具类
 *
 * @author qiunet
 *
 */
public class StringUtil {
	private StringUtil(){}
	/**汉字的正则表达式*/
	public static final Pattern CHINESE_REGEX = Pattern.compile("([\u4E00-\u9FA5]*)");
	/**
	 * IP v4<br>
	 * 采用分组方式便于解析地址的每一个段
	 */
	//String IPV4 = "\\b((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\b";
	String IPV4 = "^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)$";
	/**
	 * IP v6
	 */
	String IPV6 = "(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]+|::(ffff(:0{1,4})?:)?((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9]))";

	/**
	 * 中文字、英文字母、数字和下划线
	 */
	String GENERAL_WITH_CHINESE = "^[\u4E00-\u9FFF\\w]+$";
	/**
	 * 邮件，符合RFC 5322规范，正则来自：http://emailregex.com/
	 * What is the maximum length of a valid email address? https://stackoverflow.com/questions/386294/what-is-the-maximum-length-of-a-valid-email-address/44317754
	 * 注意email 要宽松一点。比如 jetz.chong@hutool.cn、jetz-chong@ hutool.cn、jetz_chong@hutool.cn、dazhi.duan@hutool.cn 宽松一点把，都算是正常的邮箱
	 */
	String EMAIL = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)])";
	/**
	 * 移动电话
	 * eg: 中国大陆： +86  180 4953 1399，2位区域码标示+11位数字
	 * 中国大陆 +86 Mainland China
	 */
	String MOBILE = "(?:0|86|\\+86)?1[3-9]\\d{9}";

	/**
	 * 空字符串
	 */
	public static final String EMPTY_STRING = StringUtils.EMPTY;
	/***
	 * 判断是否是空字符串
	 * @return
	 */
	public static boolean isEmpty(String str){
		return str == null || str.length() == 0;
	}
	/**
	 * 分割字符串
	 *
	 * 跟string.split() 略有不同. 不会按照正则表达式分割. 严格按照分隔符号分割字符串.
	 * 例:  ",".split(",").length = 0
	 *   	gameutil.split("," , ",").length = 2
	 *
	 * @param srcStr
	 * @param splitStr
	 * @return
	 */
	public static String [] split(String srcStr , String splitStr){
		if(srcStr == null || srcStr.length() == 0) return new String[0];

		List<String > retList = new LinkedList<>();
		int before = 0 , cursor = 0 ,validCursor;
		boolean splitValid = false;
		for(int i = 0 ; i < srcStr.length(); i ++){
			if(!splitValid && srcStr.charAt(i) == splitStr.charAt(0)) {
				splitValid = true;
				cursor = i;
			}
			if(splitValid){
				if(srcStr.charAt(i) != splitStr.charAt(i - cursor)){
					splitValid = false;
					continue;
				}
				if(i - cursor == splitStr.length()  -1){
					validCursor = cursor;
					retList.add(srcStr.substring(before, validCursor));
					before = i + 1;
					splitValid= false;
				}
			}
		}
		retList.add(srcStr.substring(before));
		return retList.toArray(new String[0]);
	}
	/***
	 * 字符串数组 转 基础数据类型数组
	 * @param <T>
	 * @param k
	 * @param t
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Number> T[] conversion(String [] k,Class<T> t)
	{
		T [] tt = (T[]) Array.newInstance(t, k.length);
		try {
		for(int i = 0 ; i < k.length; i ++){
				Method m = t.getMethod("valueOf", String.class);
				tt[i] = (T)m.invoke(t , k[i]);
		}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return tt;
	}
	/**
	 * 分隔字符串并转类型
	 * @param src
	 * @param split
	 * @param t
	 * @return
	 */
	public static <T extends Number> T[] conversion(String src , String split, Class<T> t){
		return conversion(split(src, split), t);
	}
	/**
	 * 数组拼串
	 * @param arrays
	 * @param separator
	 * @return
	 */
	public static <T> String arraysToString(T [] arrays ,String separator){
		return arraysToString(arrays, "", "", separator);
	}
	/**
	 * 数组拼串
	 * @param arrays
	 * @param separator
	 * @return
	 */
	public static <T> String arraysToString(T [] arrays , String start ,String end ,String separator){
		return  arraysToString(arrays, start, end, 0, arrays.length - 1, separator);
	}
	/**
	 * 数组拼串
	 * @param arrays
	 * @param separator
	 * @return
	 */
	public static <T> String arraysToString(T [] arrays , String start ,String end , int startIndex, int endIndex, String separator){
		StringJoiner joiner = new StringJoiner(separator, start, end);

		for(int i = startIndex; i <= endIndex; i++){
			joiner.add(objectToString(arrays[i]));
		}
		return joiner.toString();
	}

	/**
	 * 数组拼串
	 * @param arrays
	 * @param separator
	 * @return
	 */
	public static <T> String arraysToString(Collection<T> arrays, String start ,String end, String separator){
		StringJoiner joiner = new StringJoiner(separator, start, end);
		arrays.forEach(t -> joiner.add(t.toString()));
		return joiner.toString();
	}

	/**
	 * map 拼串
	 * @param keyValSeparator key val 中间分隔符号
	 * @param separator 两组key val 分割符号
	 * @param start 初始位置字符
	 * @param end 结束位置字符
	 * @return
	 */
	public static String mapToString(Map<?,?> map, String keyValSeparator, String separator, String start, String end) {
		StringJoiner joiner = new StringJoiner(separator, start, end);
		map.forEach((key, val) -> joiner.add(key.toString() + keyValSeparator + val.toString()));
		return joiner.toString();
	}
	/**
	 * map 拼串
	 * @param keyValSeparator key val 中间分隔符号
	 * @param separator 两组key val 分割符号
	 * @return
	 */
	public static String mapToString(Map<?,?> map, String keyValSeparator, String separator) {
		return mapToString(map, keyValSeparator, separator, EMPTY_STRING, EMPTY_STRING);
	}

	private static String objectToString(Object obj) {
		if (obj == null) return null;

		if (obj.getClass().isArray()) {
			return Arrays.toString((Object[]) obj);
		}
		return obj.toString();
	}
	/***
	 * 返回是否符合正则表达式
	 * @param matchStr
	 * @param regex
	 * @return
	 */
	public static boolean regex(String matchStr,String regex){
		if(StringUtil.isEmpty(matchStr) ){
			return false;
		}
		Pattern p = Pattern.compile(regex);
		return regex(matchStr, p);
	}

	/***
	 * 是不是字符串
	 * @param numStr
	 * @return
	 */
	public static boolean isNum(String numStr) {
		if (numStr == null) return false;
		return numStr.matches("-?[0-9]+");
	}
	/**
	 * 是否完美匹配
	 * @param matchStr
	 * @param p
	 * @return
	 */
	public static boolean regex(String matchStr,Pattern p){
		Matcher m = p.matcher(matchStr);
		return m.matches();
	}
	/***
	 * 返回符合正则表达式的个数
	 * @param matchStr
	 * @param regex
	 * @return
	 */
	public static int regexCount(String matchStr,String regex){
		if(StringUtil.isEmpty(matchStr) ){
			return 0;
		}
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(matchStr);
		if(m.find()){
			return m.groupCount();
		}
		return 0;
	}

	/**
	 * 是否是汉字
	 * @param matchStr
	 * @return
	 */
	public static boolean regexChinese(String matchStr){
		return regex(matchStr, CHINESE_REGEX);
	}
	 /**
	 * 格式化输出字符串
	 * 格式: format("xx{0}x{1}xx{0}x", "参数1", "参数2")
	 * 跟sformat比较, 这里的参数占位符可以重复使用.
	 * @param string
	 * @param params
	 * @return
	 */
	public static String format(String string, Object... params) {
		return MessageFormat.format(string, params);
	}

	/**
	 * 格式化字符串
	 * 格式: sformat("xx{}xxx{}xx", "参数1", "参数2")
	 * @param string
	 * @param params
	 * @return
	 */
	public static String slf4jFormat(String string, Object ... params) {
		return MessageFormatter.arrayFormat(string, params).getMessage();
	}


	public static String getIntHexVal(int val){
		return String.format("%08x", val).toUpperCase();
	}

	public static String getByteHexVal(byte val){
		return String.format("%02x", val).toUpperCase();
	}

	public static String getShortHexVal(short val){
		return String.format("%04x", val).toUpperCase();
	}

	private static final String chars =  "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

	/**
	 * 生成一定长度的随机字符串
	 * @param count
	 * @return
	 */
	public static String randomString (int count) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0 ; i < count; i++) {
			sb.append(chars.charAt(MathUtil.random(chars.length())));
		}
		return sb.toString();
	}

	/**
	 * 对string 重复 count次
	 * @param string
	 * @param count
	 * @return
	 */
	public static String repeated(String string, int count) {
		if (count == 0) {
			return StringUtil.EMPTY_STRING;
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < count; i++) {
			sb.append(string);
		}
		return sb.toString();
	}
	/**
	 * 屏蔽两端大部分空白字符
	 * @return
	 */
	public static String powerfulTrim(String str){
		if (str == null || str.isEmpty()) return str;

		int start = 0;
		int length = str.length();

		while (start < length && !isAllowChar(str.charAt(start))) {
			start ++;
		}

		while (length > start && !isAllowChar(str.charAt(length - 1))) {
			length --;
		}

		if (start > 0 || length < str.length()) {
			return str.substring(start, length);
		}
		return str;
	}

	/***
	 * 判断字符是否是允许字符
	 * @param ch
	 * @return
	 */
	private static boolean isAllowChar(char ch) {
		if (ch >= 33 && ch <=126) {
			return true;
		}
		return ch >= start && ch <= end;
	}

	private static final int start = Integer.valueOf("4e00", 16);
	private static final int end = Integer.valueOf("9fa5", 16);
	/***
	 * 计算中英文混合情况下的真实字数
	 * 汉字 占长度 2
	 * 字母 占长度 1
	 * @param input
	 * @return
	 */
	public static int getMixedStringLength(String input) {
		int length = 0;
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			length += (c >= start && c <= end ? 2 : 1);
		}
		return length;
	}
	/**
	 * <p>Compares two Strings, returning <code>true</code> if they are equal.</p>
	 *
	 * <p><code>null</code>s are handled without exceptions. Two <code>null</code>
	 * references are considered to be equal. The comparison is case sensitive.</p>
	 *
	 * <pre>
	 * StringUtils.equals(null, null)   = true
	 * StringUtils.equals(null, "abc")  = false
	 * StringUtils.equals("abc", null)  = false
	 * StringUtils.equals("abc", "abc") = true
	 * StringUtils.equals("abc", "ABC") = false
	 * </pre>
	 *
	 * @see java.lang.String#equals(Object)
	 * @param str1  the first String, may be null
	 * @param str2  the second String, may be null
	 * @return <code>true</code> if the Strings are equal, case sensitive, or
	 *  both <code>null</code>
	 */
	public static boolean equals(String str1, String str2) {
		return Objects.equals(str1, str2);
	}

}
