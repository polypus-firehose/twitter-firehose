# Maven on CentOS 7.
# Copyright (C) 2016-2019 Rodrigo Martínez <dev@brunneis.com>
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

FROM centos:7
MAINTAINER "Rodrigo Martínez" <dev@brunneis.com>

################################################
# MAVEN
################################################

ENV MAVEN_VERSION 3.6.3
ENV ARCHIVE apache-maven-$MAVEN_VERSION-bin.tar.gz
ENV ARCHIVE_URL https://dist.apache.org/repos/dist/release/maven/maven-3/$MAVEN_VERSION/binaries/$ARCHIVE
ENV SHA_URL $ARCHIVE_URL.asc
ENV MAVEN_KEYS https://dist.apache.org/repos/dist/release/maven/KEYS

# UTF-8 locale
RUN localedef -c -f UTF-8 -i en_US en_US.UTF-8
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en  
ENV LC_ALL en_US.UTF-8  

# Install Maven
RUN \
	yum -y update \
	&& yum -y install \
		wget \
		ca-certificates \
		java-1.8.0-openjdk-devel \
	&& yum clean all \
	&& wget $ARCHIVE_URL \
	&& wget $SHA_URL \
	&& wget $MAVEN_KEYS \
	&& gpg --import KEYS \
	&& gpg --verify $ARCHIVE.asc \
	&& mkdir /opt/maven \
	&& tar xvf $ARCHIVE -C /opt/maven \
	&& rm -f $ARCHIVE \
	&& rm -f $ARCHIVE.asc \
	&& rm -f KEYS \
	&& ln -s /opt/maven/*maven* /opt/maven/default
	
# Directories of binaries are added to PATH
ENV M2_HOME /opt/maven/default
ENV PATH=$M2_HOME/bin:$PATH

# Other environment variables
ENV JAVA_HOME /usr

# The container stays idle
CMD bash --login -i

ENTRYPOINT /tmp/polypus/builder/build-polypus.sh
WORKDIR /tmp/polypus
