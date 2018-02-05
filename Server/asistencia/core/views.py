from django.shortcuts import render
from django.http import HttpResponse
from django.contrib.auth.models import User, Group
from rest_framework import viewsets,generics
from .serializers import UserSerializer, GroupSerializer
from django.views.decorators.csrf import csrf_exempt
from django.db import transaction
from django.core.exceptions import ObjectDoesNotExist
import json
from django.http import JsonResponse
from django.contrib.auth import authenticate
from django.core import serializers

from .models import AsistenciaEstudiante
from .models import AsistenciaProfesor
from .models import Profesor
from .models import Estudiante
from .models import Facultad
from .models import Edificio
from .models import Aula
from .models import Arduino
from .models import Materia
from .models import Paralelo
from .models import SenalEstudiante
from .models import SenalProfesor

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
    return HttpResponse("Asistencia Automatica")

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
    queryset = AsistenciaEstudiante.objects.all()
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
def gestionar_profesor(request):
    #Insertar el inicio de las asistencias en AsistenciaProfesor
    pass

@csrf_exempt
@transaction.atomic
def login_profesor(request):
    #Insertar el inicio de las asistencias en AsistenciaProfesor
    print('entro a login profesor')
    if request.method =='POST':
        msg = json.loads(request.body)
        print(msg)
        usuario = msg.get('user')
        password = msg.get('pass')
        print(usuario)
        print(password)
        user = authenticate(username=usuario,password=password)
        if user is not None:
            profesor = Profesor.objects.filter(user__username = usuario).first()
            print(profesor)
            serial = ProfesorSerializer(profesor)
            return JsonResponse({'response': '1', 'msg': 'OK', 'profesor': serial.data})
        return JsonResponse({'response': '0', 'msg': 'NO se encontro al profesor'})
    return JsonResponse({'response': '-1', 'msg': 'NO ADMITIDO'})


@csrf_exempt
@transaction.atomic
def gestionar_estudiante(request):

    def get_info_codigo(codigo):
        return codigo[0:4], codigo[4:8], codigo[8:10], codigo[10:12]

    def validar_posicion(estudiante, senales, asistencia_estudiante):

        senales_estudiante = senales
        asistencia_profesor = AsistenciaProfesor.objects.filter(codigo = asistencia_estudiante.codigodecodificado).first()

        if not asistencia_profesor:
            raise Except("Error al obtener Asistencia del profesor")

        senales_profesor = SenalProfesor.objects.filter(asistencia = asistencia_profesor).order_by('-level2')

        if not senales_profesor:
            raise Except("Error al obtener senales WIFI del profesor")

        print('3')

        for senal_est in senales_estudiante:
            bssid_est  = senal_est.get('bssid')
            level2_est = senal_est.get('level2')

            if not bssid_est or not level2_est:
                raise Exception("Error al leer bssid o level2")

            senal_pro = senales_profesor.filter(bssid = bssid_est).first()

            if senal_pro and not senal_pro.level2 in range(level2_est - 10, level2_est + 10):
                raise Exception("Error potencia senal fuera de rango. Senal profesor(" + senal_pro.bssid + "): " + str(senal_pro.level2) +
                                ". Senal estudiante(" + bssid_est + "): " + str(level2_est))
            if senal_pro:
                print(senal_pro.ssid + ' ' + str(senal_pro.level2) + ' (' + str(level2_est - 10) + ' - ' + str(level2_est + 10) + ')')
        asistencia_estudiante.aprobado = 1
        asistencia_estudiante.save()

        return True

    if request.method == 'POST':
        aprobado = 1
        try:
            print(request.body)
            #msg_asistencias = AESCipher('51e1f539-b614-4df1-8005-96eb4b4e4b07').decrypt(request.body)

            #msg_asistencias = json.loads(msg_asistencias)
            msg_asistencias = json.loads(request.body)
            print(msg_asistencias)
            for msg_asistencia in msg_asistencias:

                print(msg_asistencia)

                codigo_decodificado = msg_asistencia.get('codigo')

                #Busco en la tabla de asistencia profesor por el codigo
                asistencia_prof = AsistenciaProfesor.objects.filter(codigo = codigo_decodificado).first()

                print('-1-')

                if not asistencia_prof:
                    aprobado = 0
                    #raise Exception('Error, Codigo decodificado no existe en tabla asistencia profesor')
                    print ('Error, Codigo decodificado no existe en tabla asistencia profesor')

                print('Codigo: ' + asistencia_prof.codigo)

                info_estudiante = msg_asistencia.get('estudiante')

                matricula   = info_estudiante.get('matricula')
                nombres     = info_estudiante.get('nombres')
                apellidos   = info_estudiante.get('apellidos')
                mac         = msg_asistencia.get('mac')
                imei        = msg_asistencia.get('imei')
                distancia_x = msg_asistencia.get('distanciaX')
                distancia_y = msg_asistencia.get('distanciaY')
                fecha       = msg_asistencia.get('fecha')
                senales     = msg_asistencia.get('senales')

                if len(senales) <= 0:
                    #raise Exception('Error no existe lectura de senales WIFI en estudiante')
                    aprobado = 0
                    print('Error no existe lectura de senales WIFI en estudiante')
                print('-2-')
                #estudiante, existe_estudiante = Estudiante.objects.get_or_create(matricula = matricula)
                estudiante = None
                try:
                    estudiante = Estudiante.objects.get(matricula = matricula)
                except ObjectDoesNotExist:
                    estudiante = Estudiante.objects.create(matricula = matricula, nombres = nombres, apellidos = apellidos, mac = mac, imei = imei)

                if not estudiante:
                    raise Exception('Error al crear/obtener estudiante')

                print ('Estudiante: ' + str(estudiante))

                  #M1      #M2       #M3a    #M3b
                materia, profesor, paralelo, aula = get_info_codigo(codigo_decodificado)
                print(materia)
                print(profesor)
                print(paralelo)
                print(aula)

                profesor = Profesor.objects.filter(identificador = profesor).first()
                print('-2,1-')
                if not profesor:
                    aprobado = 0
                    #raise Exception('Error al obtener profesor')
                    print ('Error al obtener profesor')

                materia  = Materia.objects.filter(identificador = materia).first()

                if not materia:
                    #raise Exception('Error al obtener materia')
                    aprobado = 0
                    print ('Error al obtener materia')

                paralelo = Paralelo.objects.filter(identificador = paralelo).first()

                if not paralelo:
                    #raise Exception('Error al obtener paralelo')
                    aprobado = 0
                    print ('Error al obtener paralelo')

                aula = Aula.objects.filter(identificador = aula).first()

                if not aula:
                    #raise Exception('Error al obtener aula')
                    aprobado = 0
                    print('Error al obtener aula')

                asistencia = AsistenciaEstudiante.objects.create(id_estudiante = estudiante,
                                                       id_profesor = profesor,
                                                       id_materia = materia,
                                                       id_paralelo = paralelo,
                                                       id_aula = aula,
                                                       codigodecodificado = codigo_decodificado,
                                                       distanciax = distancia_x,
                                                       distanciay = distancia_y,
                                                       fecha = fecha,
                                                       aprobado = aprobado)

                if asistencia is None:
                    raise Exception('Error al crear asistencia')

                for _senal in senales:
                    bssid  = _senal.get('bssid')
                    ssid   = _senal.get('ssid')
                    level  = _senal.get('level')
                    level2 = _senal.get('level2')

                    senal = SenalEstudiante.objects.create(bssid = bssid,
                                                             ssid = ssid,
                                                             level = level,
                                                             level2 = level2,
                                                             asistencia = asistencia)

                    if not senal:
                        raise Exception('Error al crear senal')

                aceptado = validar_posicion(estudiante, senales, asistencia)

                print ('OK')
                return JsonResponse({'response':'0', 'msg' : 'ok'})
        except Exception as e:
            print (str(e))
            return JsonResponse({'response':'1', 'msg' : str(e)})
    else:
        return JsonResponse({'response':'1', 'msg' : 'metodo no soportado'})


def codigos_asistencias(request):
    if request.method == 'GET':
        codigos = None
        try:
            codigos = AsistenciaProfesor.objects.filter().values('codigo')
            #print (list(codigos))

            return JsonResponse({'response': '0', 'codigos': list(codigos), 'msg' : 'OK'})
        except Exception as e:
            print (str(e))
            return JsonResponse({'response':'1', 'codigos': '', 'msg' : str(e)})
    else:
        return JsonResponse({'response':'1', 'codigos': '', 'msg' : 'metodo no soportado'})


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