#!/bin/bash
while(true); do netstat -ano|grep 443|grep ESTABLISHED; sleep 1; clear; done
