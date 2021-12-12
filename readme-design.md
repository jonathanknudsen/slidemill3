# Three Directories Keep State

- _1new_ holds links to images that have been recently uploaded and should be shown first
- _2pool_ holds links to images yet to be displayed
- _3done_ holds links to images that have been displayed

The images themselves are kept in a single directory for easy rsync-ing
from Kristen's laptop.

`PhotoDB` maintains this system of links, and its `clean()` method ensures
that the three directories contain only valid links, and that every image
in the image directory has a corresponding link.

# Display Algorithm

while (true)
  get next photo path from PhotoDB

  load photo
  
  wait for however much of the delay time was not taken with image loading

# Syncing from Kristen's MacBook

Run osxphotos to export
Run rsync

