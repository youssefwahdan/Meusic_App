# Fix Database Access on Main Thread in PlaylistManager

The application crashes with `java.lang.IllegalStateException: Cannot access database on the main thread` because a Room database operation is being performed within a `Handler.post(Looper.getMainLooper())` block in `PlaylistManager.addSongToPlaylistSafe`.

## User Review Required

> [!IMPORTANT]
> This change moves database write operations from the Main Thread to a background executor thread, which is required by Room. This might slightly change the timing of the `AddSongCallback` result, but it will prevent the fatal crash.

## Proposed Changes

### app sub-project

#### [MODIFY] [PlaylistManager.java](file:///home/joe/develop/projects/Java_Mobile_App/First_App/app/src/main/java/com/example/first_app/PlaylistManager.java)

Refactor `addSongToPlaylistSafe` to ensure `playlistSongDao.addSongToPlaylist` is executed on the background thread (via `executor`) instead of the main thread.

Current logic:
1. `executor` checks if song exists.
2. `Handler.post` switches to Main Thread.
3. If not exists, `playlistSongDao.addSongToPlaylist` is called (CRASH).

Proposed logic:
1. `executor` checks if song exists.
2. If exists, switch to Main Thread to call callback with failure.
3. If not exists, `executor` adds the song.
4. Then switch to Main Thread to call callback with success.

## Verification Plan

### Manual Verification
- Deploy the app.
- Attempt to add a song to a playlist.
- Verify the app no longer crashes.
- Verify that if the song is already in the playlist, the error message is correctly displayed.
- Verify that if the song is not in the playlist, it is added successfully.
