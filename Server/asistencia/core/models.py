from django.db import models
from datetime import datetime
# Create your models here.
class Profesor(models.Model):
    identificador = models.CharField(max_length=20)
    nombres = models.CharField(max_length=60)
    apellidos = models.CharField(max_length=60)
    correo = models.CharField(max_length=60)

class Estudiante(models.Model):
    matricula = models.CharField(max_length=10,primary_key=True)
    nombres = models.CharField(max_length=200)
    apellidos = models.CharField(max_length=200)
    #correo = models.CharField(max_length=60)
    imei = models.CharField(max_length=20, null=True, blank=True)
    mac = models.CharField(max_length=20, null=True, blank=True)
    #celular = models.CharField(max_length=12)

    def __str__(self):
        return self.apellidos+' '+self.nombres

class Facultad(models.Model):
    nombre= models.CharField(max_length=50)
    abreviatura=models.CharField(max_length=6)

class Edificio(models.Model):
    id_facultad=models.ForeignKey('Facultad',on_delete=None)
    nombre=models.CharField(max_length=60)
    ayuda=models.TextField(null=True)

class Aula(models.Model):
    id_edificio = models.ForeignKey('Edificio',on_delete=models.CASCADE,)
    nombre=models.CharField(max_length=10)
    descripcion=models.CharField(max_length=25)
    capacidad=models.PositiveSmallIntegerField(default=0)

class Arduino(models.Model):
    id_aula=models.ForeignKey('Aula', on_delete=models.CASCADE,)
    mac_arduino=models.CharField(max_length=17)

class Materia(models.Model):
    id_facultad=models.ForeignKey('Facultad',on_delete=None)
    codigo=models.CharField(max_length=10,unique=True)
    nombre=models.CharField(max_length=30)
    descripcion = models.CharField(max_length=100,null=True)

class Paralelo(models.Model):
    id_materia=models.ForeignKey('Materia',on_delete=None)
    id_profesor=models.ForeignKey('Profesor',on_delete=None)
    anio=models.CharField(max_length=4)
    termino=models.CharField(max_length=1)
    numero_paralelo=models.CharField(max_length=2)

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
    id_estudiante = models.ForeignKey('Estudiante', on_delete = None, null=False, blank=False, default=0)
    id_profesor = models.ForeignKey('Profesor', on_delete=models.CASCADE, null=False, blank=False, default=0)
    id_materia = models.ForeignKey('Materia', on_delete = models.CASCADE, null=False, blank=False, default=0)
    id_paralelo = models.ForeignKey('Paralelo', on_delete = models.CASCADE, null=False, blank=False, default=0)
    id_aula = models.ForeignKey('Aula', on_delete = None, null=False, blank=False, default=0)
    fecha = models.DateTimeField()
    codigodecodificado = models.CharField(max_length=200, null=True, blank=True)
    distanciax = models.FloatField(default=0.0, null=True, blank=True)
    distanciay = models.FloatField(default=0.0, null=True, blank=True)
    verificado = models.PositiveSmallIntegerField(default=0,null=False, blank=False)

class Senal(models.Model):
    id_asistencia = models.ForeignKey('Asistencia', on_delete=None)
    bssid = models.CharField(max_length=200);
    ssid = models.CharField(max_length=200);
    level = models.IntegerField(default=0)
    level2 = models.IntegerField(default=0)

class resumenAsistencias(models.Model):
    id_estudiante=models.ForeignKey('Estudiante',on_delete=None)
    id_Paralelo=models.ForeignKey('Paralelo',on_delete=None)
    diasAsistidos=models.PositiveSmallIntegerField(default=0)
    diasContados=models.PositiveSmallIntegerField(default=0)
    porcentajeFaltas=models.PositiveSmallIntegerField()

#def stringaFecha(self, hora, minutos):
#    cel = models.DateTimeField()
#    hoy = datetime.today()
#    hoy.hour=hora
#    return models.DateTimeField.__new__()




