# -*- coding: utf-8 -*-
# Generated by Django 1.11 on 2017-12-22 03:08
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
            name='Asistencia',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('IMEI', models.CharField(max_length=15)),
                ('celular_no', models.CharField(max_length=11)),
                ('paraleloid', models.CharField(max_length=2)),
                ('hora_foto', models.DateTimeField()),
            ],
        ),
        migrations.CreateModel(
            name='Aula',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('nombre', models.CharField(max_length=10)),
                ('descripcion', models.CharField(max_length=25)),
                ('capacidad', models.PositiveSmallIntegerField(default=0)),
            ],
        ),
        migrations.CreateModel(
            name='Edificio',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('nombre', models.CharField(max_length=60)),
                ('ayuda', models.TextField(null=True)),
            ],
        ),
        migrations.CreateModel(
            name='Estudiante',
            fields=[
                ('identificador', models.CharField(max_length=10, primary_key=True, serialize=False)),
                ('apellidos', models.CharField(max_length=60)),
                ('correo', models.CharField(max_length=60)),
                ('nombres', models.CharField(max_length=60)),
                ('IMEI', models.CharField(max_length=15)),
                ('celular_no', models.CharField(max_length=12)),
            ],
        ),
        migrations.CreateModel(
            name='Facultad',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('nombre', models.CharField(max_length=50)),
                ('abreviatura', models.CharField(max_length=6)),
            ],
        ),
        migrations.CreateModel(
            name='Materia',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('codigo', models.CharField(max_length=10, unique=True)),
                ('nombre', models.CharField(max_length=30)),
                ('descripcion', models.CharField(max_length=100, null=True)),
                ('id_facultad', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='core.Facultad')),
            ],
        ),
        migrations.CreateModel(
            name='Paralelo',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
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
                ('identificador', models.CharField(max_length=20)),
                ('nombres', models.CharField(max_length=60)),
                ('apellidos', models.CharField(max_length=60)),
                ('correo', models.CharField(max_length=60)),
            ],
        ),
        migrations.CreateModel(
            name='resumenAsistencias',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('diasAsistidos', models.PositiveSmallIntegerField(default=0)),
                ('diasContados', models.PositiveSmallIntegerField(default=0)),
                ('porcentajeFaltas', models.PositiveSmallIntegerField()),
                ('id_Paralelo', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='core.Paralelo')),
                ('id_estudiante', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='core.Estudiante')),
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
            model_name='asistencia',
            name='id_estudiante',
            field=models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='core.Estudiante'),
        ),
        migrations.AddField(
            model_name='asistencia',
            name='id_paralelo',
            field=models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='core.Paralelo'),
        ),
        migrations.AddField(
            model_name='arduino',
            name='id_aula',
            field=models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='core.Aula'),
        ),
    ]
