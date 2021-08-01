package de.soundboardcrafter.activity.soundboard.play;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;

import de.soundboardcrafter.R;
import de.soundboardcrafter.activity.common.AbstractPermissionFragment;
import de.soundboardcrafter.activity.common.mediaplayer.MediaPlayerService;
import de.soundboardcrafter.activity.common.mediaplayer.SoundboardMediaPlayer;
import de.soundboardcrafter.activity.sound.edit.common.SoundEditFragment;
import de.soundboardcrafter.activity.sound.edit.soundboard.play.SoundboardPlaySoundEditActivity;
import de.soundboardcrafter.dao.SoundDao;
import de.soundboardcrafter.dao.SoundboardDao;
import de.soundboardcrafter.dao.TutorialDao;
import de.soundboardcrafter.de.soundboardcrafter.widget.GridAutofitLayoutManager;
import de.soundboardcrafter.model.AssetFolderAudioLocation;
import de.soundboardcrafter.model.IAudioLocation;
import de.soundboardcrafter.model.SelectableSoundboard;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.SoundWithSelectableSoundboards;
import de.soundboardcrafter.model.Soundboard;
import de.soundboardcrafter.model.SoundboardWithSounds;

/**
 * Shows Soundboard in a Grid
 */
public class SoundboardFragment extends AbstractPermissionFragment implements ServiceConnection {
    public interface HostingActivity {
        void soundsDeleted();

        void setChangingSoundboardEnabled(boolean changingSoundboardEnabled);
    }

    private static final String TAG = SoundboardFragment.class.getName();

    private static final String ARG_SOUNDBOARD = "Soundboard";

    private static final int EDIT_SOUND_REQUEST_CODE = 1;

    private static final int TAP_TARGET_RADIUS_DP = 62;

    private static final int FIRST_ITEM_X_DP = 25;
    private static final int FIRST_ITEM_Y_DP = 25;

    // See https://developer.android.com/guide/topics/ui/menus.html and
    // https://medium.com/over-engineering/using-androids-actionmode-e903181f2ee3 .
    private final ActionMode.Callback manualSortingActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.fragment_soundboard_play_manual_sorting, menu);
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            hostingActivity.setChangingSoundboardEnabled(true);
        }
    };

    private enum SortOrder {
        BY_NAME(Comparator.comparing(Sound::getCollationKey));
        // TODO Have other sort orders?

        private final Comparator<Sound> comparator;

        SortOrder(Comparator<Sound> comparator) {
            this.comparator = comparator;
        }
    }

    private RecyclerView recyclerView;
    private SoundboardItemAdapter soundboardItemAdapter;
    private MediaPlayerService mediaPlayerService;
    private SoundboardWithSounds soundboard;
    private static final String ARG_SORT_ORDER = "sortOrder";
    private SortOrder sortOrder;

    private ActionMode actionMode;

    //  private boolean tutorialHintsAllowed;

    @Nullable
    private HostingActivity hostingActivity;

    /**
     * Creates a <code>SoundboardFragment</code> for this soundboard.
     */
    static SoundboardFragment createFragment(SoundboardWithSounds soundboard) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_SOUNDBOARD, soundboard);
        SoundboardFragment fragment = new SoundboardFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    @UiThread
    public void onServiceConnected(ComponentName name, IBinder binder) {
        MediaPlayerService.Binder b = (MediaPlayerService.Binder) binder;
        mediaPlayerService = b.getService();
        //as soon the media player service is connected, the play/stop icons can be set correctly
        updateUI();

        Log.d(TAG, "MediaPlayerService is connected");
    }

    @UiThread
    private void showTutorialHintAsNecessary() {
        final TutorialDao tutorialDao = TutorialDao.getInstance(requireContext());
        if (!tutorialDao.isChecked(TutorialDao.Key.SOUNDBOARD_PLAY_START_SOUND)) {
            showTutorialHint(
                    R.string.tutorial_soundboard_play_start_sound_description,
                    new TapTargetView.Listener() {
                        @Override
                        public void onTargetClick(TapTargetView view) {
                            super.onTargetClick(view); // dismiss view

                            @Nullable View itemView = findFirstItemView();
                            if (itemView != null) {
                                itemView.performClick();
                            }
                        }

                        @Override
                        public void onTargetLongClick(TapTargetView view) {
                            // Don't dismiss view and don't handle like a single click
                        }
                    });
        } else if (!tutorialDao.isChecked(TutorialDao.Key.SOUNDBOARD_PLAY_CONTEXT_MENU)) {
            showTutorialHint(
                    R.string.tutorial_soundboard_play_context_menu_description,
                    new TapTargetView.Listener() {
                        @Override
                        public void onTargetClick(TapTargetView view) {
                            // Don't dismiss view
                        }

                        @Override
                        public void onTargetLongClick(TapTargetView view) {
                            super.onTargetClick(view); // dismiss view

                            @Nullable View itemView = findFirstItemView();
                            if (itemView != null) {
                                // Simulate a long in the middle of the item
                                itemView.performLongClick(dp(FIRST_ITEM_X_DP), dp(FIRST_ITEM_Y_DP));
                            }
                        }
                    });
        }
    }

    private View findFirstItemView() {
        return recyclerView.findChildViewUnder(dp(FIRST_ITEM_X_DP), dp(FIRST_ITEM_Y_DP));
    }

    @UiThread
    private void showTutorialHint(
            int descriptionId, TapTargetView.Listener tapTargetViewListener) {
        @Nullable Activity activity = getActivity();

        if (activity != null) {
            TapTargetView.showFor(activity,
                    TapTarget.forBounds(
                            getTapTargetBounds(),
                            activity.getResources().getString(descriptionId))
                            .transparentTarget(true)
                            .targetRadius(TAP_TARGET_RADIUS_DP),
                    tapTargetViewListener);
        }
    }

    @NonNull
    private Rect getTapTargetBounds() {
        final int[] location = getTapTargetLocation();

        final int tapTargetRadius = dp(TAP_TARGET_RADIUS_DP);

        return new Rect(location[0] - tapTargetRadius, location[1] - tapTargetRadius,
                location[0] + tapTargetRadius, location[1] + tapTargetRadius);
    }

    @NonNull
    private int[] getTapTargetLocation() {
        final int[] location = new int[2];
        recyclerView.getLocationOnScreen(location);

        location[0] += dp(70);
        location[1] += dp(60);
        return location;
    }

    @Override
    @UiThread
    public void onServiceDisconnected(ComponentName name) {
        // TODO What to do on Service Disconnected?
    }

    @Override
    @UiThread
    public void onBindingDied(ComponentName name) {
        // TODO What to do on Service Died?
    }

    @Override
    @UiThread
    public void onNullBinding(ComponentName name) {
        // TODO What to do on Null Binding?
    }

    @Override
    @UiThread
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Bundle arguments = getArguments();
        if (arguments == null) {
            throw new IllegalStateException("SoundboardFragment without arguments");
        }
        soundboard = (SoundboardWithSounds) arguments.getSerializable(ARG_SOUNDBOARD);

        Intent intent = new Intent(getActivity(), MediaPlayerService.class);
        requireActivity().startService(intent);
        // TODO Necessary?! Also done in onResume()
        bindService();
    }

    private void bindService() {
        Intent intent = new Intent(getActivity(), MediaPlayerService.class);
        requireActivity().bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    @UiThread
    public void onPause() {
        super.onPause();

        requireActivity().unbindService(this);
    }


    @Override
    @UiThread
    public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_soundboard,
                container, false);

        Bundle args = getArguments();
        if (args != null) {
            sortOrder = (SortOrder) args.getSerializable(ARG_SORT_ORDER);
        }
        if (sortOrder == null) {
            sortOrder = SortOrder.BY_NAME;
        }

        recyclerView = rootView.findViewById(R.id.recycler_view_soundboard);

        // Changes in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        int horizontalSpacing = dp(10);
        recyclerView.addItemDecoration(
                new SoundboardItemDecoration(dp(24), horizontalSpacing, true));

        GridAutofitLayoutManager layoutManager =
                new GridAutofitLayoutManager(requireContext().getApplicationContext(),
                        dp(100), horizontalSpacing);

        recyclerView.setLayoutManager(layoutManager);

        initSoundboardItemAdapter();
        registerForContextMenu(recyclerView);

        return rootView;
    }

    private int dp(final int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                requireContext().getResources().getDisplayMetrics());
    }

    @UiThread
    private void onClickSoundboard(SoundboardItemRow soundboardItemRow, Sound sound) {
        MediaPlayerService service = getService();
        if (service == null) {
            return;
        }

        if (actionMode != null) {
            return;
        }

        if (!service.isActivelyPlaying(soundboard.getSoundboard(), sound)) {
            final IAudioLocation audioLocation = sound.getAudioLocation();

            if (audioLocation instanceof AssetFolderAudioLocation
                    || isPermissionReadExternalStorageGrantedIfNotAskForIt()) {
                soundboardItemRow.setImage(R.drawable.ic_stop);
                try {
                    service.play(soundboard.getSoundboard(), sound,
                            soundboardItemAdapter::notifyDataSetChanged);
                } catch (IOException e) {
                    soundboardItemRow.setImage(R.drawable.ic_play);
                    handleSoundFileNotFound(sound);
                }
            }
        } else {
            service.stopPlaying(soundboard.getSoundboard(), sound, true);
        }

        @Nullable final Context context = getContext();
        if (context != null) {
            TutorialDao.getInstance(context).check(TutorialDao.Key.SOUNDBOARD_PLAY_START_SOUND);
        }
    }

    @Override
    protected void onPermissionReadExternalStorageGranted() {
        // User has to click once again.
    }

    @Override
    protected void onPermissionReadExternalStorageNotGrantedUserGivesUp() {
        // Nothing happens
    }

    private void handleSoundFileNotFound(Sound sound) {
        Snackbar snackbar = Snackbar
                .make(recyclerView, getString(R.string.audiofile_not_found),
                        Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.update_all_soundboards_and_sounds),
                        view -> new DeleteSoundsTask(requireActivity()).execute(sound.getId())
                );
        snackbar.show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (actionMode != null) {
            return false;
        }

        final int id = item.getItemId();
        if (id == R.id.toolbar_menu_sound_sort_alpha) {
            new SoundSortInSoundboardTask(requireContext(), soundboard, SortOrder.BY_NAME)
                    .execute();
            return true;
        } else if (id == R.id.toolbar_menu_sound_sort_manually) {
            if (actionMode != null) {
                return false;
            }

            if (hostingActivity != null) {
                hostingActivity.setChangingSoundboardEnabled(false);
            }

            actionMode = requireAppCompatActivity()
                    .startSupportActionMode(manualSortingActionModeCallback);
            if (actionMode == null) {
                if (hostingActivity != null) {
                    hostingActivity.setChangingSoundboardEnabled(true);
                }
                return false;
            }

            actionMode.setTitle(R.string.soundboards_play_sort_manually_title);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private AppCompatActivity requireAppCompatActivity() {
        return (AppCompatActivity) requireActivity();
    }

    private SoundboardItemRow.MediaPlayerServiceCallback newMediaPlayerServiceCallback() {
        return new SoundboardItemRow.MediaPlayerServiceCallback() {
            @Override
            public boolean isConnected() {
                return getService() != null;
            }

            @Override
            public boolean isActivelyPlaying(Soundboard soundboard, Sound sound) {
                MediaPlayerService service = getService();
                if (service == null) {
                    return false;
                }
                return service.isActivelyPlaying(soundboard, sound);
            }

            @Override
            public void setOnPlayingStopped(Soundboard soundboard, Sound sound,
                                            SoundboardMediaPlayer.OnPlayingStopped onPlayingStopped) {
                MediaPlayerService service = getService();
                if (service != null) {
                    service.setOnPlayingStopped(soundboard, sound, onPlayingStopped);
                }
            }

            @Override
            public void stopPlaying(Soundboard soundboard, Sound sound, boolean fadeOut) {
                MediaPlayerService service = getService();
                if (service != null) {
                    service.stopPlaying(soundboard, sound, fadeOut);
                }
            }
        };
    }

    /**
     * Starts reading the data for the UI (first time) or
     * simply ensure that the grid shows the latest information.
     */
    @UiThread
    private void updateUI() {
        if (soundboardItemAdapter != null) {
            soundboardItemAdapter.notifyDataSetChanged();

            // if (tutorialHintsAllowed) {
            // soundboardItemAdapter.setTutorialHintsAllowed(true);
            // }
        }
    }

    @Override
    @UiThread
    // Called especially when the SoundboardPlaySoundEditActivity returns.
    public void onResume() {
        super.onResume();

        updateUI();

        if (soundboardItemAdapter.getItemCount() > 0) {
            showTutorialHintAsNecessary();
        }

        bindService();
    }

    @UiThread
    private void initSoundboardItemAdapter() {
        soundboardItemAdapter =
                new SoundboardItemAdapter(newMediaPlayerServiceCallback(), soundboard);

        soundboardItemAdapter.setActionListener(new SoundboardItemAdapter.ActionListener() {
            @Override
            @UiThread
            public void onItemClick(int position, View view) {
                if (!(view instanceof SoundboardItemRow)) {
                    return;
                }

                if (actionMode != null) {
                    return;
                }

                SoundboardItemRow soundboardItemRow = (SoundboardItemRow) view;

                onClickSoundboard(soundboardItemRow, soundboardItemAdapter.getItem(position));
            }

            @Override
            public void onCreateContextMenu(int position, ContextMenu menu) {
                if (actionMode != null) {
                    return;
                }

                onCreateSoundboardContextMenu(soundboardItemAdapter.getItem(position), menu);
            }
        });

        SoundboardSwipeAndDragCallback swipeAndDragCallback =
                new SoundboardSwipeAndDragCallback();
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeAndDragCallback);

        recyclerView.setAdapter(soundboardItemAdapter);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        updateUI();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.fragment_soundboard_play, menu);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof HostingActivity) {
            hostingActivity = (HostingActivity) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        hostingActivity = null;
    }

    private void onCreateSoundboardContextMenu(Sound sound, ContextMenu menu) {
        int soundboardId = getUniqueTabId();

        menu.add(soundboardId,
                1,
                0,
                R.string.context_menu_edit_sound);
        menu.add(soundboardId,
                2,
                1,
                R.string.context_menu_remove_sound);

        menu.setHeaderTitle(sound.getName());

        @Nullable final Context context = getContext();
        if (context != null) {
            TutorialDao.getInstance(context).check(TutorialDao.Key.SOUNDBOARD_PLAY_CONTEXT_MENU);
        }
    }

    @Override
    @UiThread
    public boolean onContextItemSelected(@Nonnull MenuItem item) {
        if (actionMode != null) {
            return false;
        }

        if (item.getGroupId() != getUniqueTabId()) {
            // The wrong fragment got the event.
            // See https://stackoverflow.com/questions/9753213/wrong-fragment-in-viewpager
            // -receives-oncontextitemselected-call
            return false; // Pass the event to the next fragment
        }

        final int id = item.getItemId();
        if (id == 1) {
            @Nullable Sound sound = soundboardItemAdapter.getContextMenuItem();
            if (sound == null) {
                return false;
            }

            Log.d(TAG, "Editing sound " + this + " \"" + sound.getName() + "\"");

            Intent intent = SoundboardPlaySoundEditActivity.newIntent(getActivity(), sound);
            startActivityForResult(intent, EDIT_SOUND_REQUEST_CODE);
            return true;
        } else if (id == 2) {
            int position = soundboardItemAdapter.getContextMenuPosition();
            if (position < 0) {
                return false;
            }

            Log.d(TAG, "Removing sound " + position);
            soundboardItemAdapter.remove(position);
            new RemoveSoundsTask(requireActivity()).execute(position);
            return true;
        } else {
            return false;
        }
    }

    private int getUniqueTabId() {
        return soundboard.getId().hashCode();
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    @UiThread
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case EDIT_SOUND_REQUEST_CODE:
                Log.d(TAG, "Editing sound " + this + ": Returned from sound edit fragment with OK");

                @Nullable String soundIdString =
                        data.getStringExtra(SoundEditFragment.EXTRA_SOUND_ID);
                if (soundIdString != null) {
                    final UUID soundId = UUID.fromString(soundIdString);
                    new UpdateSoundsTask(requireActivity()).execute(soundId);
                } else {
                    // Sound file has been deleted
                    if (hostingActivity != null) {
                        hostingActivity.soundsDeleted();
                    }
                }
                break;
        }
    }

    @Override
    @UiThread
    public void onDestroy() {
        // tutorialHintsAllowed = false;

        // TODO: 17.03.2019 destroy service save state

        super.onDestroy();
    }

    private MediaPlayerService getService() {
        if (mediaPlayerService == null) {
            // TODO Necessary?! Also done in onResume()
            bindService();
        }
        return mediaPlayerService;
    }

    /*
    public void setTutorialHintsAllowed(boolean tutorialHintsAllowed) {
        this.tutorialHintsAllowed = tutorialHintsAllowed;

        if (soundboardItemAdapter != null && !tutorialHintsAllowed) {
            soundboardItemAdapter.setTutorialHintsAllowed(false);
        }
    }
     */

    // See https://therubberduckdev.wordpress
// .com/2017/10/24/android-recyclerview-drag-and-drop-and-swipe-to-dismiss/ .
    class SoundboardSwipeAndDragCallback extends ItemTouchHelper.Callback {
        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder) {
            if (!(viewHolder instanceof SoundboardItemAdapter.ViewHolder)) {
                return 0;
            }
            int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT
                    | ItemTouchHelper.RIGHT;
            int swipeFlags = 0;
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            int from = viewHolder.getAdapterPosition();
            int to = target.getAdapterPosition();

            soundboardItemAdapter.move(from, to);

            new SoundboardFragment.MoveSoundTask(requireActivity(), from, to)
                    .execute();
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            // swiping not supported - might be a problem in a grid view
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return actionMode != null;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return false;
        }

        @Override
        public void onChildDraw(@NonNull Canvas c,
                                @NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder,
                                float dX,
                                float dY,
                                int actionState,
                                boolean isCurrentlyActive) {
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                float alpha = 1 - (Math.abs(dX) / recyclerView.getWidth());
                viewHolder.itemView.setAlpha(alpha);
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }

    class SoundSortInSoundboardTask extends AsyncTask<Void, Void, SoundboardWithSounds> {
        private final String TAG = UpdateSoundsTask.class.getName();

        private final WeakReference<Context> appContextRef;
        private final SoundboardWithSounds soundboardWithSounds;
        private final SortOrder order;

        SoundSortInSoundboardTask(Context context, SoundboardWithSounds soundboardWithSounds,
                                  SortOrder order) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
            this.soundboardWithSounds = soundboardWithSounds;
            this.order = order;
        }

        @Override
        @WorkerThread
        protected SoundboardWithSounds doInBackground(Void... v) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }


            Log.d(TAG, "Sorting sounds: " + soundboardWithSounds);
            soundboardWithSounds.sortSoundsBy(order.comparator);

            SoundboardDao.getInstance(appContext).relinkSoundsInOrder(soundboardWithSounds);

            return soundboardWithSounds;
        }

        @Override
        @UiThread
        protected void onPostExecute(SoundboardWithSounds soundboardWithSounds) {
            if (!isAdded()) {
                // fragment is no longer linked to an activity
                return;
            }
            Context appContext = appContextRef.get();

            if (appContext == null) {
                // application context no longer available, I guess that result
                // will be of no use to anyone
                return;
            }

            soundboardItemAdapter.updateSounds(soundboardWithSounds.getSounds());
        }
    }

    /**
     * A background task, used to retrieve some sounds from the database and update the GUI.
     */
    class UpdateSoundsTask extends AsyncTask<UUID, Void, Map<Sound, Boolean>> {
        private final String TAG = UpdateSoundsTask.class.getName();

        private final WeakReference<Context> appContextRef;

        UpdateSoundsTask(Context context) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
        }

        @Override
        @WorkerThread
        protected Map<Sound, Boolean> doInBackground(UUID... soundIds) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            Log.d(TAG, "Loading sounds: " + Arrays.toString(soundIds));
            Map<Sound, Boolean> sounds = new HashMap<>();
            for (UUID soundId : soundIds) {
                SoundWithSelectableSoundboards soundWithSelectableSoundboards =
                        SoundDao.getInstance(appContext)
                                .findSoundWithSelectableSoundboards(soundId);
                List<SelectableSoundboard> soundboards =
                        soundWithSelectableSoundboards.getSoundboards();
                Optional<SelectableSoundboard> foundSoundboard =
                        soundboards.stream()
                                .filter(s -> s.getSoundboard().getId().equals(soundboard.getId()))
                                .findFirst();
                if (foundSoundboard.isPresent()) {
                    if (!foundSoundboard.get().isSelected()) {
                        sounds.put(soundWithSelectableSoundboards.getSound(), false);
                    } else {
                        sounds.put(soundWithSelectableSoundboards.getSound(), true);
                    }
                }
            }

            return sounds;
        }

        @Override
        @UiThread
        protected void onPostExecute(Map<Sound, Boolean> sounds) {
            stopSoundsNotInSoundboard(sounds);
            if (!isAdded()) {
                // fragment is no longer linked to an activity
                return;
            }
            Context appContext = appContextRef.get();

            if (appContext == null) {
                // application context no longer available, I guess that result
                // will be of no use to anyone
                return;
            }


            soundboardItemAdapter.updateSounds(sounds);
        }

        private void stopSoundsNotInSoundboard(Map<Sound, Boolean> sounds) {
            for (Map.Entry<Sound, Boolean> soundEntry : sounds.entrySet()) {
                if (!soundEntry.getValue()) {
                    mediaPlayerService
                            .stopPlaying(soundboard.getSoundboard(), soundEntry.getKey(), true);
                }
            }
        }
    }

    /**
     * A background task, used to move a sound inside the soundboard
     */
    class MoveSoundTask extends AsyncTask<Void, Void, Void> {
        private final String TAG = MoveSoundTask.class.getName();

        private final WeakReference<Context> appContextRef;
        private final int oldPosition;
        private final int newPosition;

        MoveSoundTask(Context context, int oldPosition, int newPosition) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
            this.oldPosition = oldPosition;
            this.newPosition = newPosition;
        }

        @Override
        @WorkerThread
        protected Void doInBackground(Void... voids) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            SoundboardDao soundboardDao = SoundboardDao.getInstance(appContext);
            Log.d(TAG,
                    "Moving sound from position " + oldPosition +
                            " to position " + newPosition);

            soundboardDao.moveSound(
                    soundboardItemAdapter.getSoundboard().getSoundboard().getId(),
                    oldPosition, newPosition);

            return null;
        }
    }

    /**
     * A background task, used to remove sounds with the given indexes from the soundboard
     */
    class RemoveSoundsTask extends AsyncTask<Integer, Void, Void> {
        private final String TAG = RemoveSoundsTask.class.getName();

        private final WeakReference<Context> appContextRef;

        RemoveSoundsTask(Context context) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
        }

        @Override
        @WorkerThread
        protected Void doInBackground(Integer... indexes) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            SoundboardDao soundboardDao = SoundboardDao.getInstance(appContext);

            for (int index : indexes) {
                Log.d(TAG, "Removing sound + " + index + " from soundboard");

                soundboardDao.unlinkSound(
                        soundboardItemAdapter.getSoundboard().getSoundboard().getId(), index);
            }

            return null;
        }
    }

    /**
     * A background task, used to delete sounds (from the database, from all soundboards etc.)
     */
    class DeleteSoundsTask extends AsyncTask<UUID, Void, Void> {
        private final String TAG = DeleteSoundsTask.class.getName();

        private final WeakReference<Context> appContextRef;

        DeleteSoundsTask(Context context) {
            super();
            appContextRef = new WeakReference<>(context.getApplicationContext());
        }

        @Override
        @WorkerThread
        protected Void doInBackground(UUID... soundIds) {
            Context appContext = appContextRef.get();
            if (appContext == null) {
                cancel(true);
                return null;
            }

            SoundDao soundDao = SoundDao.getInstance(appContext);

            for (UUID soundId : soundIds) {
                Log.d(TAG, "Deleting sound + " + soundId);

                soundDao.delete(soundId);
            }

            return null;
        }

        @Override
        @UiThread
        protected void onPostExecute(Void nothing) {
            Context appContext = appContextRef.get();

            if (appContext == null) {
                // application context no longer available, I guess that result
                // will be of no use to anyone
                return;
            }

            if (hostingActivity != null) {
                hostingActivity.soundsDeleted();
            }
        }
    }
}
