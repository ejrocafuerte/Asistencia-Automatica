# -*- coding: utf-8 -*-
# Generated by Django 1.11 on 2018-02-05 03:47
from __future__ import unicode_literals

from django.db import migrations, models
import django.utils.timezone


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0001_initial'),
    ]

    operations = [
        migrations.AddField(
            model_name='paralelo',
            name='hora1',
            field=models.TimeField(default=django.utils.timezone.now),
            preserve_default=False,
        ),
        migrations.AddField(
            model_name='paralelo',
            name='hora2',
            field=models.TimeField(default=django.utils.timezone.now),
            preserve_default=False,
        ),
        migrations.AddField(
            model_name='paralelo',
            name='hora3',
            field=models.TimeField(default=django.utils.timezone.now),
            preserve_default=False,
        ),
    ]