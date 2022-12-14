package com.ottsz.stationpublicity.sqlite;

import java.util.LinkedHashSet;
import java.util.Set;

public class VersionFactory {

    private static final Set<String> list = new LinkedHashSet<>();

    static {
        list.add("VersionSecond");
//        list.add("VersionThird");
    }

    /**
     * 根据数据库版本号获取对应的对象
     *
     * @param version 版本号
     * @return Upgrade实现类
     */
    public static Upgrade getUpgrade(int version) {
        Upgrade upgrade = null;
        //通过反射机制获取类
        try {
            for (String className : list) {
                Class<?> cls = Class.forName(className);
                if (Upgrade.class == cls.getSuperclass()) {
                    VersionCode versionCode = cls.getAnnotation(VersionCode.class);
                    if (null == versionCode) {
                        throw new IllegalStateException(cls.getName() + "类必须使用VersionCode类注解");
                    } else {
                        if (version == versionCode.value()) {
                            upgrade = (Upgrade) cls.newInstance();
                            break;
                        }
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new IllegalStateException("没有找到类名,请检查list里面添加的类名是否正确！");
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return upgrade;
    }

    /**
     * 得到当前数据库版本
     */
    public static int getCurrentDBVersion() {
        return list.size() + 1;
    }
}
