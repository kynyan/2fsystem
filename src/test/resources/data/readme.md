2fsystem
----------------------------------------

This is a simple filesystem based on a single file.

[FileSystem](https://github.com/kynyan/2fsystem/blob/master/src/main/java/home/work/system/FileSystem.java) is used to perform all the operations, which include:
* Create file
* Copy existing file from ambient filesystem
* Download and save file using specified URL
* Read file
* Remove file
* Defragment filesystem
* Format filesystem
* List filenames of all existing files
* Check if the file exists in the system
* Get available space

[FileSystemDriver](https://github.com/kynyan/2fsystem/blob/master/src/main/java/home/work/system/FileSystemDriver.java) is a proxy to access FileSystem. 
All clients are supposed to use FileSystemDriver instance to use FileSystem.
Thread safety of FileSystem allows multiple instances of FileSystemDriver operate 
at the same time. Examples of its usage can be found in [FileSystemDriverTest](https://github.com/kynyan/2fsystem/blob/master/src/test/java/home/work/FileSystemDriverTest.java)

[File](https://github.com/kynyan/2fsystem/blob/master/src/main/java/home/work/system/File.java) is a wrapped of String filename and byte[] content which represent a file. 

## Assumptions and limitations

* Filesystem is flat, meaning there are only files, not folders. 
Hence, files with the same name are not allowed, but can be overwritten.
* Max allowed file size is limited by Integer.MAX_VALUE (about 2GB). 
This is because internal implementation of FileSystem uses int.
* If the app was using FileSystem stopped, FileSystem can be restored 
from the file "fileSystem" at the next start.
* To ensure there is only one instance of FileSystem Spring dependency injection is used

## File system structure

 ![alt text](https://github.com/kynyan/2fsystem/blob/master/src/main/resources/file_structure.jpg "File structure")