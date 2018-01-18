from django.shortcuts import render
from django.http import HttpResponse
from django.contrib.auth.models import User, Group
from rest_framework import viewsets,generics
from .serializers import UserSerializer, GroupSerializer
from django.views.decorators.csrf import csrf_exempt
from django.db import transaction
from django.core import exceptions
import json
from django.http import JsonResponse

from .models import Asistencia
from .models import Profesor
from .models import Estudiante
from .models import Facultad
from .models import Edificio
from .models import Aula
from .models import Arduino
from .models import Materia
from .models import Paralelo

from .serializers import AsistenciaSerializer
from .serializers import ProfesorSerializer
from .serializers import EstudianteSerializer
from .serializers import FacultadSerializer
from .serializers import EdificioSerializer
from .serializers import AulaSerializer
from .serializers import ArduinoSerializer
from .serializers import MateriaSerializer
from .serializers import ParaleloSerializer


# Create your views here.
def index(request):
    return HttpResponse("SITIO EN CONSTRUCCION")

class UserViewSet(viewsets.ModelViewSet):
    """
    API endpoint that allows users to be viewed or edited.
    """
    queryset = User.objects.all().order_by('-date_joined')
    serializer_class = UserSerializer


class GroupViewSet(viewsets.ModelViewSet):
    """
    API endpoint that allows groups to be viewed or edited.
    """
    queryset = Group.objects.all()
    serializer_class = GroupSerializer

class AsistenciaList(generics.ListCreateAPIView):
    queryset = Asistencia.objects.all()
    serializer_class = AsistenciaSerializer
    name = 'Asistencia-list'

class ProfesorList(generics.ListAPIView):
    queryset = Profesor.objects.all()
    serializer_class =  ProfesorSerializer
    name='Profesor-list'

class EstudianteList(generics.ListCreateAPIView):
    queryset = Estudiante.objects.all()
    serializer_class = EstudianteSerializer
    name='Estudiante-list'

class FacultadList(generics.ListAPIView):
    queryset = Facultad.objects.all()
    serializer_class =  FacultadSerializer
    name = 'Facultad-list'

class EdificioList(generics.ListAPIView):
    queryset = Edificio.objects.all()
    serializer_class = EdificioSerializer
    name = 'Edificio-list'

class AulaList(generics.ListAPIView):
    queryset = Aula.objects.all()
    serializer_class = AulaSerializer
    name = 'Aula-list'

class ArduinoList(generics.ListAPIView):
    queryset = Arduino.objects.all()
    serializer_class = ArduinoSerializer
    name = 'Arduino-list'

class MateriaList(generics.ListAPIView):
    queryset = Materia.objects.all()
    serializer_class = MateriaSerializer
    name = 'Materia-list'

class ParaleloList(generics.ListAPIView):
    queryset = Paralelo.objects.all()
    serializer_class = ParaleloSerializer
    name ='Paralelo-list'


@csrf_exempt
@transaction.atomic
def gestionar(request):

    if request.method == 'POST':
        try:
            msg_asistencia = AESCipher('51e1f539-b614-4df1-8005-96eb4b4e4b07').decrypt(request.body)
            msg_asistencia = json.loads(msg_asistencia)[0]
            print(msg_asistencia)

            info_estudiante = msg_asistencia.get('estudiante')

            matricula = info_estudiante.get('matricula')
            nombres   = info_estudiante.get('nombres')
            apellidos = info_estudiante.get('apellidos')
            mac       = info_estudiante.get('mac')
            imei      = info_estudiante.get('imei')

            print(info_estudiante)

            #estudiante, existe_estudiante = Estudiante.objects.get_or_create(matricula = matricula)
            estudiante = None
            try:
                estudiante = Estudiante.objects.get(matricula = matricula)
            except DoesNotExist:
            #if not existe_estudiante:
                estudiante = Estudiante.objects.create(matricula = matricula, nombres = nombres, apellidos = apellidos, mac = mac, imei = imei)

            if estudiante is None:
                raise Exception('Error al crear/obtener estudiante')

            print ('Estudiante' + estudiante)

            codigo_decodificado = msg_asistencia.get('codigo')

              #M1      #M2       #M3a    #M3b
            materia, profesor, paralelo, aula = get_info_codigo(codigo_decodificado)

            distancia_x = msg_asistencia.get('distanciaX')
            distancia_y = msg_asistencia.get('distanciaY')
            fecha = msg_asistencia.get('fecha')

            print(materia)
            print(profesor)
            print(paralelo)
            print(aula)

            asistencia = Asistencia.objects.create(id_estudiante = estudiante,
                                                   id_profesor = profesor,
                                                   id_paralelo = paralelo,
                                                   id_aula = aula,
                                                   codigodecodificado = codigo_decodificado,
                                                   distanciax = distancia_x,
                                                   distanciay = distancia_y,
                                                   fecha = fecha,
                                                   verificado = 0)

            if asistencia is None:
                raise Exception('Error al crear asistencia')

            senales = msg_asistencia.get('senales')

            for _senal in senales:
                bssid  = _senal.get('bssid')
                ssid   = _senal.get('ssid')
                level  = _senal.get('level')
                level2 = _senal.get('level2')

                senal = Senal.objects.create(bssid = bssid,
                                             ssid = ssid,
                                             level = level,
                                             level2 = level2,
                                             id_asistencia = asistencia)

                if senal is null:
                    raise Exception('Error al crear senal')

            aceptado = validar_asistencia(estudiante, senales, asistencia)

            print ('OK')
            return JsonResponse({'response':'0'})
        except Exception as e:
            print (str(e))
            return JsonResponse({'response':'1'})
    else:
        return JsonResponse({'response':'1'})

    def get_info_codigo(codigo):
        return codigo[0:3], codigo[4,7], codigo[8,9], codigo[10,11]

    def validar_asistencia(estudiante, senales, asistencia):
        return true



#INSTALAR DEPENDENCIA
#pip install pycryptodome

import base64
import hashlib
from Crypto import Random
from Crypto.Cipher import AES
from base64 import b64decode

class AESCipher:

    def __init__(self, key):
        self.bs = 16
        self.key = hashlib.sha256(key.encode('utf-8')).digest()

    #def encrypt(self, message):
    #    message = self._pad(message)
    #    iv = Random.new().read(AES.block_size)
    #    cipher = AES.new(self.key, AES.MODE_CBC, iv)
    #    return base64.b64encode(iv + cipher.encrypt(message)).decode('utf-8')

    def decrypt(self, enc):
        enc = base64.b64decode(enc)
        iv = enc[:AES.block_size]

        cipher = AES.new(self.key, AES.MODE_CBC, iv)
        return self._unpad(cipher.decrypt(enc[AES.block_size:])).decode('utf-8')

    #def _pad(self, s):
    #    return s + (self.bs - len(s) % self.bs) * chr(self.bs - len(s) % self.bs)

    @staticmethod
    def _unpad(s):
        return s[:-ord(s[len(s)-1:])]


"""
class ParaleloAnioList(generics.ListAPIView):
    def anio(self, request):
        request
    queryset = Paralelo.objects.filter(Paralelo.anio==2017)
    serializer_class =  ParaleloSerializer
    name = 'Paralelo-list 2017'"""