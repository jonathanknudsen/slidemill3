# Three Directories Keep State

- _1new_ holds images that have been recently uploaded and should be shown first
- _2pool_ holds images yet to be displayed
- _3done_ holds images that have been displayed

# Display Algorithm

while (true)
  if _1new_ has anything in it, src = _1new_
  else if _2pool_ has anything in it, src = _2pool_
  else
    move everything in _3done_ to _2pool_
    src = _2pool_

  select a file at random
  move the file to _3done_
  return the _3done_ path to be loaded and displayed
  
  wait for the delay time

# Syncing from Kristen's MacBook

Do the Phoshare thing
Compare local directory list with consolidated _1new_, _2pool_, and _3done_ listings
Copy anything local that's not on the remote to _1new_
Remove anything that's present remotely but not locally

