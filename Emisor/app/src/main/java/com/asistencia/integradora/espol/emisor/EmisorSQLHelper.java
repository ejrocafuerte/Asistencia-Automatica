package com.asistencia.integradora.espol.emisor;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by erick on 28/12/2017.
 */

public class EmisorSQLHelper extends SQLiteOpenHelper{
    public static final String emisor_db = "emisor.db";
    private static final int version = 1;
    private static final String facultad = "CREATE TABLE core_facultad (id integer NOT NULL PRIMARY KEY AUTOINCREMENT, nombre varchar(50) NOT NULL, abreviatura varchar(6) NOT NULL)";
    private static final String materia= "CREATE TABLE core_materia (id integer NOT NULL PRIMARY KEY AUTOINCREMENT, codigo varchar(10) NOT NULL UNIQUE, nombre varchar(30) NOT NULL, descripcion varchar(100) NULL)";
    private static final String paralelo= "CREATE TABLE core_paralelo (id integer NOT NULL PRIMARY KEY AUTOINCREMENT, anio varchar(4) NOT NULL, termino varchar(1) NOT NULL, numero_paralelo varchar(2) NOT NULL)";
    private static final String profesor="CREATE TABLE core_profesor (id integer NOT NULL PRIMARY KEY AUTOINCREMENT, identificador varchar(20) NOT NULL, nombres varchar(60) NOT NULL, apellidos varchar(60) NOT NULL, correo varchar(60) NOT NULL)";
    private static final String arduino="CREATE TABLE core_arduino (id integer NOT NULL PRIMARY KEY AUTOINCREMENT,mac_arduino varchar(17) NOT NULL, id_aula_id integer NOT NULL REFERENCES core_aula (id))";

    public EmisorSQLHelper(Context ctx) {
        super(ctx,emisor_db,null,version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(facultad);
        db.execSQL(materia);
        db.execSQL(profesor);
        db.execSQL(paralelo);
        db.execSQL(arduino);
        db.execSQL("insert into core_profesor values(1,\"201021839\",\"erick joel\",\"rocafuerte villon\",\"example@example.com\")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
    public void insertFacultad(int id,String nombreF, String abreviatura){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null){
            //db.execSQL('"insert into core_facultad values"+"');
        }

    }
    public String getProfesor(){
        SQLiteDatabase db = getReadableDatabase();
        if(db!=null){
            Cursor c = db.rawQuery("select nombres, apellidos from core_profesor",null);
            if(c.moveToFirst()){
                return c.getString(0) + " "+c.getString(1);
            }
            return "error, no habia ningun profesor que es raro";
        }
        return "la base no existe";
    }
}
