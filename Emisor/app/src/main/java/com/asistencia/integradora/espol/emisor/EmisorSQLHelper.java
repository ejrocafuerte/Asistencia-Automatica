package com.asistencia.integradora.espol.emisor;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by erick on 28/12/2017.
 */

public class EmisorSQLHelper extends SQLiteOpenHelper{
    public static final String emisor_db = "emisor.db";
    private static final int version = 1;
    private static final String facultad = "CREATE TABLE core_facultad (id integer NOT NULL PRIMARY KEY AUTOINCREMENT," +
            " nombre varchar(50) NOT NULL, abreviatura varchar(6) NOT NULL)";
    private static final String materia= "CREATE TABLE core_materia (id integer NOT NULL PRIMARY KEY AUTOINCREMENT, " +
            "codigo varchar(10) NOT NULL UNIQUE, nombre varchar(30) NOT NULL, descripcion varchar(100) NULL," +
            "id_facultad_id integer NOT NULL)";
    private static final String paralelo="CREATE TABLE core_paralelo (id integer NOT NULL PRIMARY KEY AUTOINCREMENT,"+
            "identificador varchar(10) NOT NULL UNIQUE, anio varchar(4) NOT NULL, termino varchar(1) NOT NULL, "+
            "numero_paralelo varchar(2) NOT NULL, dia1 varchar(3) NOT NULL, dia2 varchar(3) NULL,"+
            "id_materia_id integer NOT NULL, id_profesor_id integer NOT NULL, hora1 time NOT NULL, hora2 time NOT NULL,"+
            "hora3 time NOT NULL, dia3 varchar(3) NULL)";
    private static final String profesor="CREATE TABLE core_profesor (id integer NOT NULL PRIMARY KEY AUTOINCREMENT, " +
            "identificador varchar(20) NOT NULL, nombres varchar(60) NOT NULL, apellidos varchar(60) NOT NULL, " +
            "correo varchar(60) NOT NULL)";
    private static final String arduino="CREATE TABLE core_arduino (id integer NOT NULL PRIMARY KEY AUTOINCREMENT," +
            "mac_arduino varchar(17) NOT NULL)";
    private static final String aula= "CREATE TABLE core_aula (id integer NOT NULL PRIMARY KEY AUTOINCREMENT, "+
            "identificador varchar(10) NOT NULL UNIQUE, nombre varchar(10) NOT NULL, descripcion varchar(25) NOT NULL,)";

    public EmisorSQLHelper(Context ctx, String nombre_db, CursorFactory cursor, int version) {
        super(ctx,nombre_db,cursor,version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(facultad);
        db.execSQL(materia);
        db.execSQL(profesor);
        db.execSQL(aula);
        db.execSQL(paralelo);
        db.execSQL(arduino);
        db.execSQL("insert into core_profesor values(1,\"0039\",\"erick joel\",\"rocafuerte villon\",\"example@example.com\")");
        db.execSQL("insert into core_facultad values(1,\"1\",\"ingenieria en electricidad y computacion \",\"FIEC\")");
        db.execSQL("insert into core_materia values(1,\"0045\",\"Inteligencia Artificial\",\"fundamentos\",1)");
        db.execSQL("insert into core_paralelo values (1,\"1\",\"2017\",\"2\",\"10\",\"LUN\",\"MIE\",\"1\",\"1\",\"09:30:00\",\"09:30:00\",\"10:30:00\", \"VIE\")");

        db.execSQL("insert into core_aula values(1,\"11\",\"1\",\"aula 1 edificio 16\",1)");
        db.execSQL("insert into core_arduino values (1,\"98:D3:32:21:0C:A4\",1)");

    }
    public void instanciar(SQLiteDatabase db){
        db.execSQL(facultad);
        db.execSQL(materia);
        db.execSQL(profesor);
        db.execSQL(paralelo);
        db.execSQL(arduino);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }/*
    public void insertFacultad(int id,String nombreF, String abreviatura){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null){
            db.execSQL("insert into core_facultad (id, nombre, apellido) values(id,nombreF,abreviatura)");
            db.close();
            System.out.println("insertado Facultad: "+nombreF);
        }else{
            System.out.print("No se pudo insertar");
        }
    }
    public void insertMateria(int id, String codigoM, String nombreM, String des,int idFac){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null){
            Cursor c = db.rawQuery("select nombre, abreviatura from core_facultad where id=idFac",null);
            if(c.moveToFirst()){
                System.out.println("Facultad encontrada, asociando");
                db.execSQL("insert into core_materia (id, codigo, nombre, descripcion,id_facultad_id) values(id,codigoM,nombreM,des,idFac)");
                db.close();
            }else{
                System.out.println("ERROR GRAVISIMO FACULTAD NO ENCONTRADA");
            }
        }else{
            System.out.print("Base CAida");
        }
    }
    public void insertParalelo(int id, String anio, String termino, String nPar,int idMateria){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null){
            Cursor c = db.rawQuery("select nombre from core_materia where id=id_materia_id",null);
            if(c.moveToFirst()){
                System.out.println("MAteria encontrada, insertando paralelo");
                db.execSQL("insert into core_paralelo (id, anio, termino, numero_paralelo,id_materia_id) values(id,anio,termino,nPar,idMateria)");
                db.close();
                System.out.println("insertado Paralelo: "+nPar+", de la Materia: "+c.getString(c.getColumnIndexOrThrow("nombre")));
                c.close();
            }else{
                System.out.println("ERROR GRAVISIMO MATERIA NO ENCONTRADA");
            }
        }else{
            System.out.print("No se puede conectar a la BD");
        }
    }
    public void insertArduino(int id, String mac){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null){
            db.execSQL("insert into core_paralelo (id, anio, termino, numero_paralelo,id_materia_id) values(id,anio,termino,nPar,idMateria)");
            db.close();
        }else{
            System.out.print("No se puede conectar a la BD");
        }
    }
    */
    public String getProfesor(){
        SQLiteDatabase db = getReadableDatabase();
        if(db!=null){
            Cursor c = db.rawQuery("select nombres, apellidos from core_profesor",null);
            db.close();
            if(c.moveToFirst()){
                return c.getString(0) + " "+c.getString(1);
            }
            return "error, no habia ningun profesor que es raro";
        }
        return "la base no existe";
    }
}
