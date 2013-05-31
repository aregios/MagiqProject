
 1. Setup OpenCV as described in "A Comprehensive Guide to Installing and Configuring OpenCV 2.pdf".
 2. IMCS2 requires PHP5.4. Copy and extract php-5.4.11.tar.bz2 into ~/src directory. Name the folder as php-5.4.11-src.
 3. Change into ~/src/php-5.4.11-src/ directory and run "./configure" command.
 4. Copy php5411.pc into /usr/lib/pkgconfig/ directory.
 5. Copy php_imcs2 folder into ~/src/ directory and change to ~/src/php_imcs2/ directory.
 6. Open the text file build_x86_64.txt copy and paste into command line and wait for build to complete.
 7. Sudo-copy the file "php_imcs2.so" into "/usr/lib/php5/20100525" directory.
 8. Add the line "extension=php_imcs2.so" at the end of php.ini file.
 9. Restart apache server.
10. Verify the extension is loaded via phpinfo.php page.