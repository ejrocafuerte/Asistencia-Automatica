from django.db import models


# Create your models here.
class Profesor(models.Model):
    identificador = models.CharField(max_length=20)
    nombres = models.CharField(max_length=60)
    apellidos = models.CharField(max_length=60)
    correo = models.CharField(max_length=60)
    nombres = models.CharField(max_length=60)

class Estudiante(models.Model):
    identificador = models.CharField(max_length=10,primary_key=True)
    nombres = models.CharField(max_length=60)
    apellidos = models.CharField(max_length=60)
    correo = models.CharField(max_length=60)
    nombres = models.CharField(max_length=60)
    IMEI = models.CharField(max_length=15)
    celular_no = models.CharField(max_length=12)

class Facultad(models.Model):
    nombre= models.CharField(max_length=50)
    abreviatura=models.CharField(max_length=6)

class Edificio(models.Model):
    id_facultad=models.ForeignKey('Facultad')
    nombre=models.CharField(max_length=60)
    ayuda=models.TextField(null=True)

class Aula(models.Model):
    id_edificio = models.ForeignKey('Edificio',on_delete=models.CASCADE,)
    nombre=models.CharField(max_length=10)
    descripcion=models.CharField(max_length=25)
    capacidad=models.PositiveSmallIntegerField(default=0)

class Materia(models.Model):
    id_facultad=models.ForeignKey('Facultad')
    codigo=models.CharField(max_length=10,unique=True)
    nombre=models.CharField(max_length=30)
    descripcion = models.CharField(max_length=100,null=True)

class Paralelo(models.Model):
    id_materia=models.ForeignKey('Materia')
    id_profesor=models.ForeignKey('Profesor')
    anio=models.CharField(max_length=4)
    termino=models.CharField(max_length=1)

    LUNES='LUN',
    MARTES='MAR',
    MIERCOLES='MIE',
    JUEVES='JUE',
    VIERNES='VIE',
    SABADO='SAB',
    dias_opt=((LUNES,'LUNES'),
              (MARTES,'MARTES'),
              (MIERCOLES,'MIERCOLES'),
              (JUEVES,'JUEVES'),
              (VIERNES,'VIERNES'),
              (SABADO,'SABADO'))
    dia1=models.CharField(max_length=3,choices=dias_opt,default=LUNES)
    dia2=models.CharField(max_length=3,choices=dias_opt,default=LUNES,null=True)
    dia3 = models.CharField(max_length=3, choices=dias_opt, default=LUNES, null=True)

class Asistencia(models.Model):
    id_paralelo=models.ForeignKey('Paralelo',on_delete=models.CASCADE)
    id_estudiante=models.ForeignKey('Estudiante')
    #desde aqui datos capturados de la aplicacion capturador#
    IMEI=models.CharField(max_length=15)
    celular_no= models.CharField(max_length=11)
    



class resumenAsistencias(models.Model):
    id_estudiante=