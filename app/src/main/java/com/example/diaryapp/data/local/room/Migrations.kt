package com.example.diaryapp.data.local.room

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// v2 → v3: 폴더를 별도 테이블(folders)로 관리하기 시작한 버전
// SQL은 schemas/.../3.json의 createSql과 반드시 같아야 한다 (Room이 마이그레이션 후 스키마를 검증함)
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `folders` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`userName` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL)"
        )

        // v2에서는 폴더 이름이 diary.folder에만 있었다 → 드로어가 읽는 folders 테이블로 옮긴다
        // 폴더 없음은 NULL(처음부터 미지정)과 ''(폴더 삭제로 비워짐) 두 형태라 둘 다 제외
        db.execSQL(
            "INSERT INTO `folders` (`userName`, `name`) " +
                "SELECT DISTINCT `userName`, `folder` FROM `diary` " +
                "WHERE `folder` IS NOT NULL AND `folder` != ''"
        )
    }
}
