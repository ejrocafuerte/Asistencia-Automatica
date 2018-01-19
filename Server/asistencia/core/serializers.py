from django.contrib.auth.models import User, Group
from rest_framework import serializers
from .models import AsistenciaEstudiante
from .models import Profesor
from .models import Estudiante
from .models import Facultad
from .models import Edificio
from .models import Aula
from .models import Arduino
from .models import Materia
from .models import Paralelo

class UserSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = User
        fields = ('url','username','email','groups')

class GroupSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = Group
        fields=('url','name')

class AsistenciaSerializer(serializers.ModelSerializer):
    class Meta:
        model = AsistenciaEstudiante
        fields = (
            'id_paralelo',
            'id_estudiante',
            'IMEI',
            'celular_no',
            'paraleloid',
            'hora_foto',
        )

class ProfesorSerializer(serializers.ModelSerializer):
    class Meta:
        model = Profesor
        fields = (
            'identificador',
            'nombres',
            'apellidos',
            'correo',
        )

class EstudianteSerializer(serializers.ModelSerializer):
    class meta:
        model = Estudiante
        fields = (
            'identificador',
            'nombres',
            'apellidos',
            'correo',
            'nombres',
            'IMEI',
            'celular_no',
        )

class FacultadSerializer(serializers.ModelSerializer):
    class meta:
        model = Facultad
        fields=(
            'nombre',
            'abreviatura',
        )

class EdificioSerializer(serializers.ModelSerializer):
    class meta:
        model = Edificio
        fields = (
            'id_facultad',
            'nombre',
            'ayuda',
        )

class AulaSerializer(serializers.ModelSerializer):
    class meta:
        model = Aula
        fields = (
            'id_edificio',
            'nombre',
            'descripcion',
            'capacidad',
        )

class ArduinoSerializer(serializers.ModelSerializer):
    class meta:
        model = Arduino
        fields = (
            'id_aula',
            'mac_arduino',
        )

class MateriaSerializer(serializers.ModelSerializer):
    class meta:
        model = Materia
        fields = (
            'id_facultad',
            'codigo,'
            'nombre',
            'descripcion',
        )

class ParaleloSerializer(serializers.ModelSerializer):
    class meta:
        model = Paralelo
        fields = (
            'id_materia',
            'id_profesor',
            'anio',
            'termino',
            'numero_paralelo',

        )
        """
class resumenAsistencias(models.Model):
    id_estudiante=models.ForeignKey('Estudiante',on_delete=None)
    id_Paralelo=models.ForeignKey('Paralelo',on_delete=None)
    diasAsistidos=models.PositiveSmallIntegerField(default=0)
    diasContados=models.PositiveSmallIntegerField(default=0)
    porcentajeFaltas=models.PositiveSmallIntegerField()
"""
#def stringaFecha(self, hora, minutos):
#    cel = models.DateTimeField()
#    hoy = datetime.today()
#    hoy.hour=hora
#    return models.DateTimeField.__new__()




