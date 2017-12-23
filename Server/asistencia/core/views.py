from django.shortcuts import render
from django.http import HttpResponse
from django.contrib.auth.models import User, Group
from rest_framework import viewsets,generics
from .serializers import UserSerializer, GroupSerializer

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

class EstudianteList(generics.ListAPIView):
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
"""
class ParaleloAnioList(generics.ListAPIView):
    def anio(self, request):
        request
    queryset = Paralelo.objects.filter(Paralelo.anio==2017)
    serializer_class =  ParaleloSerializer
    name = 'Paralelo-list 2017'"""