<!DOCTYPE html>
<html>  
    <head>    
        <meta name="viewport" content="initial-scale=1.0, user-scalable=no" />    

        <style type="text/css">      
            html { height: 100% }      
            body { height: 100%; margin: 0; padding: 0 }      
            #map_canvas { height: 100% }    
        </style>

        <script type="text/javascript" 
                src="https://maps.googleapis.com/maps/api/js?v=3.exp&sensor=false">
        </script>    

        <script type="text/javascript">      

            function initialize() {        
                
                var phpError = 0;
                var phpErrorDescription = "";

            <?php
                require_once("globals.php");

                function getMasterQueryStatement($db, $m_id, $c_id) {
                    $query = sprintf("select %s, %s from %s 
                                      where %s = %d and %s = '%s' limit 0, 1;",                        
                            mysqli_real_escape_string($db, FLD_LATITUDE),
                            mysqli_real_escape_string($db, FLD_LONGITUDE),
                            mysqli_real_escape_string($db, TABLE_IMAGE_QUERIES),
                            mysqli_real_escape_string($db, FLD_ID),
                            mysqli_real_escape_string($db, $m_id),
                            mysqli_real_escape_string($db, FLD_CLIENT_ID),
                            mysqli_real_escape_string($db, $c_id));

                    return $query;
                }


                function getDataPointsQueryStatement($db, $m_id) {
                    $query = sprintf("select %s, %s, %s from %s, %s where %s = %s and %s = %d;",
                            mysqli_real_escape_string($db, FLD_LATITUDE),
                            mysqli_real_escape_string($db, FLD_LONGITUDE),                        
                            mysqli_real_escape_string($db, FLD_DESCRIPTION),
                            mysqli_real_escape_string($db, TABLE_IMAGE_DATA),
                            mysqli_real_escape_string($db, TABLE_IMAGE_RESULTS),
                            mysqli_real_escape_string($db, FLD_DID),
                            mysqli_real_escape_string($db, FLD_ID),
                            mysqli_real_escape_string($db, FLD_MID),
                            mysqli_real_escape_string($db, $m_id));

                    return $query; 
                }


                try {
                    ini_set("display_errors", 0);                                                                       //Suppress error reporting on web page 
                    ini_set("log_errors", 1);                                                                           //Enable error logging to file
                    ini_set("error_log", ERROR_LOG);                                                                    //Specify error log file

                    if (!array_key_exists(GKEY_CID, $_GET)) {
                        throw new Exception("Missing or invalid parameter value");
                    }
                    
                    $c_id = $_GET[GKEY_CID];                                                                            //Get parameter (client id)

                    if (!array_key_exists(GKEY_MID, $_GET) || !is_numeric($_GET[GKEY_MID])) {
                        throw new Exception("Missing or invalid parameter value");
                    }              
                    
                    $m_id = $_GET[GKEY_MID];                                                                            //Get parameter (master query selector)
                    $m_title = "You are here";

                    $db = getDatabaseConnection();                                                                      //Connect to the database    
                    if (mysqli_connect_errno($db) != 0) {
                        throw new Exception(mysqli_connect_error($db));
                    }

                    $m_query = getMasterQueryStatement($db, $m_id, $c_id);                                              //Build master query
                    $m_result = mysqli_query($db, $m_query);                                                            //Execute select statement
                    if (mysqli_errno($db) != 0) {
                        throw new Exception(mysqli_error($db));
                    }

                    if (mysqli_num_rows($m_result) == 0) {
                        throw new Exception("Invalid parameter value");            
                    }

                    $m_row = mysqli_fetch_array($m_result, MYSQLI_ASSOC);                                               //Read master query data (master row)
                    $m_latitude  = $m_row[FLD_LATITUDE];
                    $m_longitude = $m_row[FLD_LONGITUDE];
                    mysqli_free_result($m_result);                                                                      //Release result set resources
                    
                    echo "\n";
                    echo "\t var masterLat = "   . json_encode($m_latitude, JSON_NUMERIC_CHECK)  . ";\n";
                    echo "\t var masterLon = "   . json_encode($m_longitude, JSON_NUMERIC_CHECK) . ";\n";
                    echo "\t var masterTitle = " . json_encode($m_title) . ";\n";
                    echo "\n";
                    
                    $d_query = getDataPointsQueryStatement($db, $m_id);                                                 //Build data points query
                    $d_result = mysqli_query($db, $d_query);                                                            //Execute select statement
                    if (mysqli_errno($db) != 0) {
                        throw new Exception(mysqli_error($db));
                    }

                    $pointsLat = array();
                    $pointsLon = array();  
                    $pointsTitles = array();

                    while ($d_row = mysqli_fetch_array($d_result, MYSQLI_ASSOC)) {                                      //Read data row
                        $pointsLat[]    = $d_row[FLD_LATITUDE];
                        $pointsLon[]    = $d_row[FLD_LONGITUDE];
                        $pointsTitles[] = ifEmptyString($d_row[FLD_DESCRIPTION], DEFAULT_DESCRIPTION);                    
                    }

                    mysqli_free_result($d_result);                                                                      //Release result set resources
                    mysqli_close($db);                                                                                  //Close database connection

                    echo "\n";
                    echo "\t var pointsLat = "   . json_encode($pointsLat, JSON_NUMERIC_CHECK) . ";\n";
                    echo "\t var pointsLon = "   . json_encode($pointsLon, JSON_NUMERIC_CHECK) . ";\n";
                    echo "\t var pointsTitle = " . json_encode($pointsTitles) . ";\n";
                    echo "\n";
                    
                } catch (Exception $ex) {
                    echo "\n";
                    echo "\t var phpError = 1;\n";
                    echo "\t var phpErrorDescription=" . json_encode($ex->getMessage()) . ";\n";
                    echo "\n";
                }   
            ?>
                
                if (phpError != 0) {
                    document.writeln(phpErrorDescription);
                    return;
                }
                
                var mapOptions = {  center: new google.maps.LatLng(masterLat, masterLon),
                                    zoom: 16,
                                    mapTypeId: google.maps.MapTypeId.ROADMAP,

                                    zoomControl: true,
                                    zoomControlOptions: { style: google.maps.ZoomControlStyle.LARGE },
                                    panControl:false,    
                                    mapTypeControl: false,  
                                    scaleControl: false,  
                                    rotateControl: false,
                                    streetViewControl: false,  
                                    overviewMapControl: true };

                var map = new google.maps.Map(document.getElementById("map_canvas"), mapOptions);

                var pointIcon = new google.maps.MarkerImage("http://chart.apis.google.com/chart?chst=d_map_pin_letter&chld=%E2%80%A2|FE7569");
                for (var i = 0; i <= pointsLat.length; i++) {
                    var dataPoint = new google.maps.LatLng(pointsLat[i], pointsLon[i]); 
                    var dataMarker = new google.maps.Marker( { position: dataPoint, 
                                                               title: pointsTitle[i], 
                                                               icon: pointIcon,
                                                               map: map});
                }

                var masterIcon = new google.maps.MarkerImage("http://chart.apis.google.com/chart?chst=d_map_pin_letter&chld=%E2%80%A2|56A5EC");
                var masterPoint = new google.maps.LatLng(masterLat, masterLon); 
                var masterMarker = new google.maps.Marker( { position: masterPoint, 
                                                             title: masterTitle, 
                                                             icon: masterIcon,
                                                             map: map});   
            }
            
        </script>    
    </head>
    
    <body onload="initialize()">
        <div id="map_canvas" style="width:100%; height:100%"></div>    
    </body>
</html>
