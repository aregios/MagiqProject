<?php

    if (!extension_loaded("imcs2")) {
        throw new Exception("Module was not loaded");
    }

    $path = "C:\\CDrive\\Backup.06.Msc\\3rd Semester Take2\\03 OpenCV\\Win32\\02 MSVC2008\\Projects\\imcs2\\Debug\\";

    $arr = array("01a.jpg", "01b.jpg", "02a.jpg", "02b.jpg", "04a.jpg", "04b.jpg");	

    $imcs = new IMCSEngine();

    $resp = $imcs->prepare($path . "02a.jpg", 3, 8);

    if ($resp!= null && $resp["status"] == 0) {	

        foreach ($arr as $value) {

            $resc = $imcs->compare($path . $value);

            if ($resc!= null && $resc["status"] == 0) {
                echo "<br>" . $resc["message"];
            } else {
                echo "<br>" . "Error: " . $resc["message"];
            }
        }
        
    } else {
        echo "<br>" . "Error: " . $resp["message"];
    }

?>
