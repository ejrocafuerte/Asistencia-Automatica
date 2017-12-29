from django.shortcuts import render
from django.http import HttpResponse
from django.contrib.auth.models import User, Group
from rest_framework import viewsets,generics
from .serializers import UserSerializer, GroupSerializer
from django.views.decorators.csrf import csrf_exempt

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
def gestionar(request):
    if request.method == 'POST':
        mensajeencriptado = (request.body[1:][:-3])# + "'"
        print(mensajeencriptado)
        key = '51e1f539-b614-4df1-8005-96eb4b4e4b07'
        mensaje = AESCipher(key).decrypt(mensajeencriptado)
        print(mensaje)
        return HttpResponse("ASISTENCIA OK")
    else:
        return HttpResponse("FORBIDDEN METHOD")

#INSTALAR DEPENDENCIA
#pip install pycryptodome

import base64
import hashlib
from Crypto import Random
from Crypto.Cipher import AES
from hashlib import md5
from base64 import b64decode

# Padding for the input string --not
# related to encryption itself.
BLOCK_SIZE = 16  # Bytes
pad = lambda s: s + (BLOCK_SIZE - len(s) % BLOCK_SIZE) * \
                chr(BLOCK_SIZE - len(s) % BLOCK_SIZE)
unpad = lambda s: s[:-ord(s[len(s) - 1:])]


class AESCipher:
    """
    Usage:
        c = AESCipher('password').encrypt('message')
        m = AESCipher('password').decrypt(c)
    Tested under Python 3 and PyCrypto 2.6.1.
    """

    def __init__(self, key):
        self.key = md5(key.encode('utf8')).hexdigest()

    def encrypt(self, raw):
        raw = pad(raw)
        iv = Random.new().read(AES.block_size)
        cipher = AES.new(self.key, AES.MODE_CBC, iv)
        return b64encode(iv + cipher.encrypt(raw))

    def decrypt(self, enc):
        enc = b64decode(enc)
        iv = enc[:16]
        cipher = AES.new(self.key, AES.MODE_CBC, iv)
        return unpad(cipher.decrypt(enc[16:])).decode('utf8')
'''
class AESCipher:

    def __init__(self, key):
        self.bs = 16
        self.key = hashlib.sha256(key.encode('utf-8')).digest()

    def encrypt(self, message):
        message = self._pad(message)
        iv = Random.new().read(AES.block_size)
        cipher = AES.new(self.key, AES.MODE_CBC, iv)
        return base64.b64encode(iv + cipher.encrypt(message)).decode('utf-8')

    def decrypt(self, enc):
        enc = base64.b64decode(enc)
        iv = enc[:AES.block_size]
        cipher = AES.new(self.key, AES.MODE_CBC, iv)
        return self._unpad(cipher.decrypt(enc[AES.block_size:])).decode('utf-8')

    def _pad(self, s):
        return s + (self.bs - len(s) % self.bs) * chr(self.bs - len(s) % self.bs)

    @staticmethod
    def _unpad(s):
        return s[:-ord(s[len(s)-1:])]

'''

"""
class ParaleloAnioList(generics.ListAPIView):
    def anio(self, request):
        request
    queryset = Paralelo.objects.filter(Paralelo.anio==2017)
    serializer_class =  ParaleloSerializer
    name = 'Paralelo-list 2017'"""