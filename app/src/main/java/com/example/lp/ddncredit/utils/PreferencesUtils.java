package com.example.lp.ddncredit.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.lp.ddncredit.Myapplication;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * 对SharedPreference的使用做了建议的封装，
 * 对外公布出put，get，remove，clear等等方法；
 * 注意一点，里面所有的commit操作使用了SharedPreferencesCompat.apply进行了替代
 * 目的是尽可能的使用apply代替commit 首先说下为什么，
 * 因为commit方法是同步的，并且我们很多时候的commit操作都是UI线程中，
 * 毕竟是IO操作，尽可能异步； 所以我们使用apply进行替代，apply异步的进行写入；
 * 但是apply相当于commit来说是new API呢，为了更好的兼容，做了适配；
 */
public class PreferencesUtils {
	/**
	 * 保存在手机里面的文件名
	 */
	public static final String FILE_NAME = "NuoShua";
	public static SharedPreferences sp = Myapplication.getInstance().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
	public static SharedPreferences.Editor editor = sp.edit();
	/**
	 * 保存数据的方法，我们需要拿到保存数据的具体类型，然后根据类型调用不同的保存方法
	 * SharedPreferences pref = context.getSharedPreferences(
	 SHAREPREFERENCE_NAME, Context.MODE_PRIVATE |Context.MODE_MULTI_PROCESS);
	 * @param key
	 * @param object
	 */
	@SuppressWarnings("unchecked")
	public static void put(String key, Object object) {

		if (object != null) {

			if (object instanceof String) {
				editor.putString(key, (String) object);
			} else if (object instanceof Integer) {
				editor.putInt(key, (Integer) object);
			} else if (object instanceof Boolean) {
				editor.putBoolean(key, (Boolean) object);
			} else if (object instanceof Float) {
				editor.putFloat(key, (Float) object);
			} else if (object instanceof Long) {
				editor.putLong(key, (Long) object);
			} else if (object instanceof Set<?>) {
				editor.putStringSet(key, (Set<String>) object);
			} else {
				editor.putString(key, object.toString());
			}
			SharedPreferencesCompat.apply(editor);
		}
	}

	/**
	 * 得到保存数据的方法，我们根据默认值得到保存的数据的具体类型，然后调用相对于的方法获取值
	 * 
	 * @param key
	 * @param defaultObject
	 * @return
	 */
	public static Object get(String key, Object defaultObject) {
		if (defaultObject instanceof String) {
			return sp.getString(key, (String) defaultObject);
		} else if (defaultObject instanceof Integer) {
			return sp.getInt(key, (Integer) defaultObject);
		} else if (defaultObject instanceof Boolean) {
			return sp.getBoolean(key, (Boolean) defaultObject);
		} else if (defaultObject instanceof Float) {
			return sp.getFloat(key, (Float) defaultObject);
		} else if (defaultObject instanceof Long) {
			return sp.getLong(key, (Long) defaultObject);
		}
		return null;
	}

	public static Boolean getBoolean(String key, Boolean defaultValue) {
		return sp.getBoolean(key, defaultValue);
	}

	public static String getString(String key, String defaultValue) {
		return sp.getString(key, defaultValue);
	}

	public static Integer getInt(String key, int defaultValue) {

		return sp.getInt(key, defaultValue);
	}

	public static Float getFloat(String key, float defaultValue) {
		return sp.getFloat(key, defaultValue);
	}

	public static Long getLong(String key, long defaultValue) {
		return sp.getLong(key, defaultValue);
	}

	/**
	 * 移除某个key值已经对应的值
	 * @param key
	 */
	public static void remove(String key) {
		editor.remove(key);
		SharedPreferencesCompat.apply(editor);
	}

	/**
	 * 清除所有数据
	 *
	 */
	public static void clear() {
		editor.clear();
		editor.commit();
		SharedPreferencesCompat.apply(editor);
	}

	/**
	 * 查询某个key是否已经存在
	 *
	 * @param key
	 * @return
	 */
	public static boolean contains(String key) {
		return sp.contains(key);
	}

	/**
	 * 返回所有的键值对
	 *
	 * @return
	 */
	public static Map<String, ?> getAll() {
		return sp.getAll();
	}

	/**
	 * 创建一个解决SharedPreferencesCompat.apply方法的一个兼容类
	 * 
	 */
	private static class SharedPreferencesCompat {
		private static final Method sApplyMethod = findApplyMethod();

		/**
		 * 反射查找apply的方法
		 * 
		 * @return
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		private static Method findApplyMethod() {
			try {
				Class clz = SharedPreferences.Editor.class;
				return clz.getMethod("apply");
			} catch (NoSuchMethodException e) {
			}

			return null;
		}

		/**
		 * 如果找到则使用apply执行，否则使用commit
		 * 
		 * @param editor
		 */
		public static void apply(SharedPreferences.Editor editor) {
			try {
				if (sApplyMethod != null) {
					sApplyMethod.invoke(editor);
					return;
				}
			} catch (IllegalArgumentException e) {
			} catch (IllegalAccessException e) {
			} catch (InvocationTargetException e) {
			}
			editor.commit();
		}
	}

}
