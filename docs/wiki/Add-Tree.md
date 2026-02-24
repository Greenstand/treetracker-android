To add a tree the planter clicks on 'Add Tree' button on the Maps view.
This flow is common for both planting a new tree as well as tracking an existing tree. The determination of whether a plant is new or existing one to track its growth is determined by the analysis of the prior tree data synced to the cloud servers.

## Convergence Parameter

Before the user is allowed to take a picture, the GPS data of the device should converge to increase the accuracy of the tree's location that helps in determining various aspects of tree tracking such as updates to existing trees versus a new tree.

The convergence here is standard deviation of last 'n' set of locations obtained since the 'Add a tree' use case is triggered.  The standard deviation should be within the threshold value set for the maximum allowed longitude and latitude parameters. If the convergence doesn't occur within a specified configured time (say 60 seconds), allow the planter to take the picture anyways.

When the user clicks on 'Add Tree' button,  the planter is taken to the image capture screen.  A spinning wheel is shown while the convergence process is in progress. If convergence is succesful, spinning wheel disappears and the planter is able to take the picture. If the timeout occurs (convergence didn't happen within the configured timeout value), the planter is allowed to take the picture.