from django.contrib import admin

# Register your models here.
from django.contrib import admin
from .models import Facultad
from .models import Materia
from .models import Paralelo
from .models import Edificio
from .models import Aula
from .models import Estudiante
from .models import Profesor
from .models import Arduino
from .models import Asistencia

# Register your models here.
admin.site.register(Facultad)
admin.site.register(Materia)
admin.site.register(Paralelo)
admin.site.register(Edificio)
admin.site.register(Aula)
admin.site.register(Estudiante)
admin.site.register(Profesor)
admin.site.register(Arduino)
admin.site.register(Asistencia)