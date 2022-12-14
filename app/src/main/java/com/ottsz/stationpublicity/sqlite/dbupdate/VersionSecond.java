package com.ottsz.stationpublicity.sqlite.dbupdate;

import android.database.sqlite.SQLiteDatabase;

import com.ottsz.stationpublicity.sqlite.Upgrade;
import com.ottsz.stationpublicity.sqlite.VersionCode;

@VersionCode(2)
public class VersionSecond extends Upgrade {
    @Override
    public void update(SQLiteDatabase db) {
        //数据库版本升级时会执行这个方法
        //第一步将表A重命名为temp_A
        //第二步创建新表A,此时表结构已加了2列
        //第三步讲temp_A表中的数据插入到表A
        //第四步删除临时表temp_A

    }
}
