# solution_viewer
Note: This is just a Proof of Concepts with a bare minimum of functionality.

Prerequsites
------------
Java 11 or later

Running
-------
Run it with: java -jar solution-viewer-0.1.jar <BurrTools file>

Controls
--------
* Move the camera with the mouse when holding down button. 
* Mouse wheel to zoom
* Right arrow to go forward (one disassembly move) and left arrow to go back. If you want to use it for assembly it is easiest to start with left arrow as it will "wrap around", ie from start position (assembled) it will go to the last move. 
* Space will toggle playing multiple moves in the current direction
* Up and down arrow to adjust speed.

Development ideas
-----------------
* UI for loading BurrTools-files from with app. This would also mean that the app can be run by just double-clicking
* UI for selecting which problem to view solutions for
* UI for selecting which solution to view
* Add buttons in addition to key bindings
* Improve animation when pieces are removed
* Improve camera to ensure that it stays focused on puzzle and that it is possible to view from all angles
* Stable piece colors or make it posssible to choose colors
* Save settings as window size to file
* Toggle piece visibility
* Android distribution including touch controls
* IOS distribution
