# -*- coding: utf-8 -*-
# Generated by Django 1.11 on 2018-02-05 03:35
from __future__ import unicode_literals

from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    initial = True

    dependencies = [
    ]

    operations = [
        migrations.CreateModel(
            name='Arduino',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('mac_arduino', models.CharField(max_length=17)),
            ],
        ),
        migrations.CreateModel(
            name='AsistenciaEstudiante',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('fecha', models.DateTimeField()),
                ('codigodecodificado', models.CharField(blank=True, max_length=200, null=True)),
                ('distanciax', models.FloatField(blank=True, default=0.0, null=True)),
                ('distanciay', models.FloatField(blank=True, default=0.0, null=True)),
                ('aprobado', models.PositiveSmallIntegerField(default=0)),
            ],
        ),
        migrations.CreateModel(
            name='AsistenciaProfesor',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('fecha', models.DateTimeField()),
                ('codigo', models.CharField(blank=True, max_length=200, null=True)),
            ],
        ),
        migrations.CreateModel(
            name='Aula',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('identificador', models.CharField(default=0, max_length=10, unique=True)),
                ('nombre', models.CharField(max_length=10)),
                ('descripcion', models.CharField(max_length=25)),
                ('capacidad', models.PositiveSmallIntegerField(default=0)),
            ],
        ),
        migrations.CreateModel(
            name='Edificio',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('identificador', models.CharField(default=0, max_length=10, unique=True)),
                ('nombre', models.CharField(max_length=60)),
                ('ayuda', models.TextField(null=True)),
            ],
        ),
        migrations.CreateModel(
            name='Estudiante',
            fields=[
                ('matricula', models.CharField(max_length=10, primary_key=True, serialize=False)),
                ('nombres', models.CharField(max_length=200)),
                ('apellidos', models.CharField(max_length=200)),
                ('imei', models.CharField(blank=True, max_length=20, null=True)),
                ('mac', models.CharField(blank=True, max_length=20, null=True)),
            ],
        ),
        migrations.CreateModel(
            name='Facultad',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('identificador', models.CharField(default=0, max_length=10, unique=True)),
                ('nombre', models.CharField(max_length=50)),
                ('abreviatura', models.CharField(max_length=6)),
            ],
        ),
        migrations.CreateModel(
            name='Materia',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('identificador', models.CharField(default=0, max_length=10, unique=True)),
                ('nombre', models.CharField(max_length=30)),
                ('descripcion', models.CharField(max_length=100, null=True)),
                ('id_facultad', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='core.Facultad')),
            ],
        ),
        migrations.CreateModel(
            name='Paralelo',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('identificador', models.CharField(default=0, max_length=10, unique=True)),
                ('anio', models.CharField(max_length=4)),
                ('termino', models.CharField(max_length=1)),
                ('numero_paralelo', models.CharField(max_length=2)),
                ('dia1', models.CharField(choices=[(('LUN',), 'LUNES'), (('MAR',), 'MARTES'), (('MIE',), 'MIERCOLES'), (('JUE',), 'JUEVES'), (('VIE',), 'VIERNES'), (('SAB',), 'SABADO')], default=('LUN',), max_length=3)),
                ('dia2', models.CharField(choices=[(('LUN',), 'LUNES'), (('MAR',), 'MARTES'), (('MIE',), 'MIERCOLES'), (('JUE',), 'JUEVES'), (('VIE',), 'VIERNES'), (('SAB',), 'SABADO')], default=('LUN',), max_length=3, null=True)),
                ('dia3', models.CharField(choices=[(('LUN',), 'LUNES'), (('MAR',), 'MARTES'), (('MIE',), 'MIERCOLES'), (('JUE',), 'JUEVES'), (('VIE',), 'VIERNES'), (('SAB',), 'SABADO')], default=('LUN',), max_length=3, null=True)),
                ('id_materia', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='core.Materia')),
            ],
        ),
        migrations.CreateModel(
            name='Profesor',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('identificador', models.CharField(default=0, max_length=20, unique=True)),
                ('nombres', models.CharField(max_length=60)),
                ('apellidos', models.CharField(max_length=60)),
                ('correo', models.CharField(max_length=60)),
            ],
        ),
        migrations.CreateModel(
            name='SenalEstudiante',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('bssid', models.CharField(max_length=200)),
                ('ssid', models.CharField(max_length=200)),
                ('level', models.IntegerField(default=0)),
                ('level2', models.IntegerField(default=0)),
                ('asistencia', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='core.AsistenciaEstudiante')),
            ],
        ),
        migrations.CreateModel(
            name='SenalProfesor',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('bssid', models.CharField(max_length=200)),
                ('ssid', models.CharField(max_length=200)),
                ('level', models.IntegerField(default=0)),
                ('level2', models.IntegerField(default=0)),
                ('asistencia', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='core.AsistenciaProfesor')),
            ],
        ),
        migrations.AddField(
            model_name='paralelo',
            name='id_profesor',
            field=models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='core.Profesor'),
        ),
        migrations.AddField(
            model_name='edificio',
            name='id_facultad',
            field=models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='core.Facultad'),
        ),
        migrations.AddField(
            model_name='aula',
            name='id_edificio',
            field=models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='core.Edificio'),
        ),
        migrations.AddField(
            model_name='asistenciaprofesor',
            name='id_aula',
            field=models.ForeignKey(default=0, on_delete=django.db.models.deletion.CASCADE, to='core.Aula'),
        ),
        migrations.AddField(
            model_name='asistenciaprofesor',
            name='id_materia',
            field=models.ForeignKey(default=0, on_delete=django.db.models.deletion.CASCADE, to='core.Materia'),
        ),
        migrations.AddField(
            model_name='asistenciaprofesor',
            name='id_paralelo',
            field=models.ForeignKey(default=0, on_delete=django.db.models.deletion.CASCADE, to='core.Paralelo'),
        ),
        migrations.AddField(
            model_name='asistenciaprofesor',
            name='id_profesor',
            field=models.ForeignKey(default=0, on_delete=django.db.models.deletion.CASCADE, to='core.Profesor'),
        ),
        migrations.AddField(
            model_name='asistenciaestudiante',
            name='id_aula',
            field=models.ForeignKey(default=0, on_delete=django.db.models.deletion.CASCADE, to='core.Aula'),
        ),
        migrations.AddField(
            model_name='asistenciaestudiante',
            name='id_estudiante',
            field=models.ForeignKey(default=0, on_delete=django.db.models.deletion.CASCADE, to='core.Estudiante'),
        ),
        migrations.AddField(
            model_name='asistenciaestudiante',
            name='id_materia',
            field=models.ForeignKey(default=0, on_delete=django.db.models.deletion.CASCADE, to='core.Materia'),
        ),
        migrations.AddField(
            model_name='asistenciaestudiante',
            name='id_paralelo',
            field=models.ForeignKey(default=0, on_delete=django.db.models.deletion.CASCADE, to='core.Paralelo'),
        ),
        migrations.AddField(
            model_name='asistenciaestudiante',
            name='id_profesor',
            field=models.ForeignKey(default=0, on_delete=django.db.models.deletion.CASCADE, to='core.Profesor'),
        ),
        migrations.AddField(
            model_name='arduino',
            name='id_aula',
            field=models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='core.Aula'),
        ),
    ]
