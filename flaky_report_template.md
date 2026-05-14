# Flaky Test Report

**Name:** Pabon Galindo, Maria Alejandra

**Name:** Quadri, Ashhad

## Flaky Test 1

**Test name:** `de.seuhd.worldcup.FileBettingServiceTest#fresh service has no bets`

**Root cause:**
The service class `FileBettingService` needs a file to write down the Bets. In the tests class
`FileBettingServiceTest`, a file is created but the tests (`fresh service has no bets` and `save bets to the shared file`)
are accessing the same file (`SHARED_BET_FILE`). Because the order of the tests is random (`@TestMethodOrder(MethodOrderer.Random::class)`),
if the test `save bets to the shared file` is executed first, the test `fresh service has no bets` will fail because the shared file has
already data on it.

**Fix:**
Using a unique temporal file per test instead of a shared one would fix the test, because now it does not matter
the order of execution, because each test will have its own file.

## Flaky Test 2

**Test name:** `de.seuhd.worldcup.FileBettingServiceTest#test file betting with threads`

**Root cause:**
The test is creating two threads that are accessing the same shared file, but the threads are not
synchronized creating a race condition. The service function `placeBet` starts  with a file read,
then the modification and finally the write-in the file; because the threads are sharing the same 
file, the threads can overlap at any point in the same file

**Fix:**
It's enough to just add at the function `placeBet` in the  `FileBettingService` class 
the decorator `@Synchronized`, this help us to "Locking" the threads, making sure that only one
thread can execute that method at a time.

## Flaky Test 3

**Test name:** `de.seuhd.worldcup.WorldCupTest#load json from network`

**Root cause:**
The function `loadJsonFromNetwork` in the class `JsonLoader` loads a json from some urls in the network.
Fetching this data from the network can be non-deterministic, sometimes can fail due to network issues, it can take more 
than the 300ms imposed by the `@timeout`.


**Fix:**
In the test, is better to replace the real network fetching with the injectable dependency `UrlFetcher` that reads
the local file. So instead of trying to read the json from the network, it uses the injection and reads the local 
file.

```
val localFetcher = UrlFetcher { _ ->
        JsonLoader::class.java.getResourceAsStream("/world_cup_2026_full_data.json")!!
    }
```
This will avoid the dependency from the network, which is a non-deterministic source, using this injectable
dependency that was already in the given code.

## Flaky Test 4

**Test name:** `de.seuhd.worldcup.WorldCupTest#evaluate returns zero when no bets are placed`

**Root cause:**
The class `BettingService` works as a singleton with a global state shared among all tests.
This class has also a cache that stores the last result from method `evaluate()`. When the method `clear()` in that class
is called, the bets are cleared but the cache no. So, when other test called this `evaluate()`, the cache had data, and
when the `clear()` method was called before each test, the cache was never empty

**Fix:**
Inside the `clear()` method, just add the cache cleaning part, cleaning all shared state, not only bets from that class

## Flaky Test 5

**Test name:** `de.seuhd.worldcup.WorldCupTest#standings are stable when multiple teams tie on all criteria`

**Root cause:**
Here, the `StandingsService` uses an `IdentityHashMap`, this map does not guarantee any iteration order, 
is a non-deterministic data structure. In the test, when the teams 'Alpha' and 'Beta' ties in everything, the
order of the result can change between executions making the test flaky

**Fix:**
In the method `calculate()` in the class `StandingsService`, we just change the return so when there is a tie,
the name of the team would serve as a tiebreaker. So, when there ir a tie, the alphabetic order by team name
ensures always the same result.