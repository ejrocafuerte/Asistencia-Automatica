from django.conf.urls import url

from . import views

urlpatterns = [
    url(r'^$',views.index),
    #url(r'^asistencia/$', views.AsistenciaList.as_view(), name='views.AsistenciaList.name'),
    url(r'^profesor/$', views.ProfesorList.as_view(), name='views.ProfesorList.name'),
    url(r'^estudiante/$',views.EstudianteList.as_view(),name='views.EstudianteList.name'),
    url(r'^facultad/$',views.FacultadList.as_view(),name='views.FacultadList.name'),
    url(r'^edificio/$',views.EdificioList.as_view(),name='views.EdificioList.name'),
    url(r'^aula/$',views.AulaList.as_view(),name='views.AulaList.name'),
    url(r'^arduino/$',views.ArduinoList.as_view(),name='views.ArduinoList.name'),
    url(r'^materia/$',views.MateriaList.as_view(),name='views.MateriaList.name'),
    url(r'^paralelo/$',views.ParaleloList.as_view(),name='views.ParaleloList.name'),
    url(r'^gestionarestudiante/$', views.gestionar_estudiante,name='gestionar_estudiante'),
    url(r'^gestionarprofesor/$', views.gestionar_profesor,name='gestionar_profesor'),
    url(r'^codigosasistencias/$', views.codigos_asistencias, name='codigos_asistencias'),
    #url(r'^paralelo/<int=anio>/<int=paralelo>/$',views.ParaleloAnioList.as_view(),name='views.ParaleloList.name'),
]