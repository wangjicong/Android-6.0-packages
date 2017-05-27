#!/usr/bin/perl

use File::Find;
use File::Basename;
use IO::File;
use Cwd;
use File::Copy;
use File::Path;
use File::Copy;
use File::Spec;

my $path_curf = File::Spec->rel2abs(__FILE__);
print "C PATH = ",$path_curf,"\n";
my ($vol, $dirs, $file) = File::Spec->splitpath($path_curf);
print "C Dir = ", $dirs,"\n";

#!Androidmainfest.xml
system("mv -f ${dirs}sprd/AndroidManifest.xml ${dirs}");

#!res
system("cp -rf ${dirs}sprd/res ${dirs}");
system("rm -rf ${dirs}sprd/res");

#src
system("cp -rf ${dirs}sprd/src/ ${dirs}");
system("rm -rf ${dirs}sprd");
