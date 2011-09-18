/**
 * 
 */

package com.gmail.charleszq.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.gmail.charleszq.FlickrViewerApplication;
import com.gmail.charleszq.R;
import com.gmail.charleszq.actions.GetActivitiesAction;
import com.gmail.charleszq.actions.IAction;
import com.gmail.charleszq.actions.ShowAuthDialogAction;
import com.gmail.charleszq.actions.ShowFavoritesAction;
import com.gmail.charleszq.actions.ShowInterestingPhotosAction;
import com.gmail.charleszq.actions.ShowMyContactsAction;
import com.gmail.charleszq.actions.ShowPeoplePhotosAction;
import com.gmail.charleszq.event.IImageDownloadDoneListener;
import com.gmail.charleszq.task.GetUserInfoTask;
import com.gmail.charleszq.utils.Constants;
import com.gmail.charleszq.utils.ImageUtils;

/**
 * Represents the fragment to be shown at the left side of the screen, which
 * acts as the main menu.
 * 
 * @author charles
 */
public class MainNavFragment extends Fragment {

	/**
	 * the handler.
	 */
	private Handler mHandler = new Handler();

	/**
	 * The item click listner to handle the main menus.
	 */
	private OnItemClickListener mItemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {

			if (position != 2) {
				// position == 2 means the gallery/set/group of mine, at this
				// time, we don't need to clear the right panel.
				cleanFragmentBackStack();
			}

			ListView list = (ListView) parent;
			list.setItemChecked(position, true);

			FlickrViewerApplication app = (FlickrViewerApplication) getActivity()
					.getApplication();
			String token = app.getFlickrToken();
			String tokenSecret = app.getFlickrTokenSecrent();

			switch (position) {
			case 0:
				ShowInterestingPhotosAction action = new ShowInterestingPhotosAction(
						getActivity());
				action.execute();
				break;
			case 1:
				ShowPeoplePhotosAction photosAction = new ShowPeoplePhotosAction(
						getActivity(), null, app.getUserName());
				if (token == null || tokenSecret == null) {
					ShowAuthDialogAction ia = new ShowAuthDialogAction(
							getActivity(), photosAction);
					ia.execute();
				} else {
					photosAction.execute();
				}

				break;
			case 2: // my photo sets and groups
				IAction switchAction = new IAction() {
					@Override
					public void execute() {
						FragmentManager fm = getActivity().getFragmentManager();
						FragmentTransaction ft2 = fm.beginTransaction();
						Fragment setFragment = new PhotoCollectionFragment();
						ft2.replace(R.id.nav_frg, setFragment);
						ft2.addToBackStack(Constants.USER_COLL_BACK_STACK);
						ft2.commit();
					}
				};
				if (token == null || tokenSecret == null) {
					ShowAuthDialogAction ia = new ShowAuthDialogAction(
							getActivity(), switchAction);
					ia.execute();
				} else {
					switchAction.execute();
				}

				break;
			case 3: // contacts
				ShowMyContactsAction contactAction = new ShowMyContactsAction(
						getActivity());
				if (token == null || tokenSecret == null) {
					ShowAuthDialogAction cia = new ShowAuthDialogAction(
							getActivity(), contactAction);
					cia.execute();
				} else {
					contactAction.execute();
				}
				break;
			case 4:
				ShowFavoritesAction favAction = new ShowFavoritesAction(
						getActivity(), null);
				if (token == null || tokenSecret == null) {
					ShowAuthDialogAction showAuthAction = new ShowAuthDialogAction(
							getActivity(), favAction);
					showAuthAction.execute();
				} else {
					favAction.execute();
				}
				break;
			case 5:
				GetActivitiesAction aaction = new GetActivitiesAction(
						getActivity());
				if (token == null || tokenSecret == null) {
					ShowAuthDialogAction showAuthAction = new ShowAuthDialogAction(
							getActivity(), aaction);
					showAuthAction.execute();
				} else {
					aaction.execute();
				}
				break;
			case 6:
				Fragment frag = new SettingsFragment();
				FragmentTransaction ft = getFragmentManager()
						.beginTransaction();
				ft.replace(R.id.main_area, frag);
				ft.addToBackStack(Constants.SETTING_BACK_STACK);
				ft.commit();
				break;
			}
		}
	};

	/**
	 * Clears all the framgment back stack, the right panel can show only the
	 * current selected contents.
	 */
	private void cleanFragmentBackStack() {
		FragmentManager fm = getFragmentManager();
		fm.popBackStack(Constants.PHOTO_LIST_BACK_STACK,
				FragmentManager.POP_BACK_STACK_INCLUSIVE);
		fm.popBackStack(Constants.SETTING_BACK_STACK,
				FragmentManager.POP_BACK_STACK_INCLUSIVE);
		fm.popBackStack(Constants.CONTACT_BACK_STACK,
				FragmentManager.POP_BACK_STACK_INCLUSIVE);
		fm.popBackStack(Constants.ACTIVITY_BACK_STACK,
				FragmentManager.POP_BACK_STACK_INCLUSIVE);
		fm.popBackStack(Constants.HELP_BACK_STACK,
				FragmentManager.POP_BACK_STACK_INCLUSIVE);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.main_nav, null);

		// main menu
		final ListView list = (ListView) view.findViewById(R.id.list_menu);
		NavMenuAdapter adapter = new NavMenuAdapter(getActivity(),
				createNavCommandItems());
		list.setAdapter(adapter);
		list.setOnItemClickListener(mItemClickListener);

		handleUserPanel(view);
		return view;
	}

	/**
	 * Deals with the user info panel in the main navigation page, showing the
	 * user information, and fetch the buddy icons.
	 * 
	 * @param view
	 *            the root view of this fragment, that is, the view returned
	 *            from <code>onCreateView</code>.
	 */
	protected void handleUserPanel(View view) {

		// the user panel.
		final View userPanel = view.findViewById(R.id.user_panel);
		final FlickrViewerApplication app = (FlickrViewerApplication) getActivity()
				.getApplication();
		String token = app.getFlickrToken();
		String tokenSecret = app.getFlickrTokenSecrent();
		userPanel
				.setVisibility(token == null || tokenSecret == null ? View.INVISIBLE
						: View.VISIBLE);
		if (token != null && tokenSecret != null) {
			TextView userText = (TextView) view.findViewById(R.id.user_name);
			userText.setText(app.getUserName());
		}

		// logout button
		ImageView button = (ImageView) view.findViewById(R.id.buttonLogout);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						getActivity());
				builder
						.setMessage(
								getActivity().getString(R.string.logout_msg))
						.setCancelable(false).setPositiveButton(
								getActivity().getString(R.string.btn_yes),
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int id) {
										app.logout();
										userPanel.setVisibility(View.INVISIBLE);
										goHome();
									}

									private void goHome() {
										FragmentManager fm = getActivity()
												.getFragmentManager();
										for (int i = 0; i < fm
												.getBackStackEntryCount(); i++) {
											fm.popBackStack();
										}
									}
								}).setNegativeButton(
								getActivity().getString(R.string.btn_no),
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int id) {
										dialog.cancel();
									}
								});
				AlertDialog alert = builder.create();
				alert.show();
			}
		});

		// buddy icon
		ImageView iconImage = (ImageView) view.findViewById(R.id.user_icon);
		Bitmap cachedIcon = getBuddyIconFromCache();
		if (cachedIcon != null) {
			iconImage.setImageBitmap(cachedIcon);
		} else {
			String userId = app.getUserId();
			GetUserInfoTask task = new GetUserInfoTask(iconImage, null,
					mImageDownloadedListener);
			task.execute(userId);
		}
	}

	/**
	 * Gets the buddy icon from the sd card, which was cached before.
	 * 
	 * @return
	 */
	private Bitmap getBuddyIconFromCache() {
		File root = new File(Environment.getExternalStorageDirectory(),
				Constants.SD_CARD_FOLDER_NAME);
		File buddyIconFile = new File(root,
				Constants.FLICKR_BUDDY_IMAGE_FILE_NAME);
		if (!buddyIconFile.exists()) {
			return null;
		}
		return BitmapFactory.decodeFile(buddyIconFile.getAbsolutePath());
	}

	/**
	 * The image download listener to save the buddy icons.
	 */
	private IImageDownloadDoneListener mImageDownloadedListener = new IImageDownloadDoneListener() {

		@Override
		public void onImageDownloaded(final Bitmap bitmap) {
			File root = new File(Environment.getExternalStorageDirectory(),
					Constants.SD_CARD_FOLDER_NAME);
			if (!root.exists() && !root.mkdirs()) {
				return;
			}

			final File buddyIconFile = new File(root,
					Constants.FLICKR_BUDDY_IMAGE_FILE_NAME);
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					ImageUtils.saveImageToFile(buddyIconFile, bitmap);
				}
			});
		}
	};

	/**
	 * The main menu adapter.
	 */
	private static class NavMenuAdapter extends BaseAdapter {

		private List<CommandItem> commands;
		private Context context;

		public NavMenuAdapter(Context context, List<CommandItem> commands) {
			this.commands = commands;
			this.context = context;
		}

		@Override
		public int getCount() {
			return commands.size();
		}

		@Override
		public Object getItem(int arg0) {
			return commands.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				LayoutInflater li = LayoutInflater.from(context);
				view = li.inflate(R.layout.main_nav_item, null);
			}
			ViewHolder holder = (ViewHolder) view.getTag();
			CommandItem command = (CommandItem) getItem(position);

			ImageView image;
			TextView text;
			if (holder == null) {
				image = (ImageView) view.findViewById(R.id.nav_item_image);
				text = (TextView) view.findViewById(R.id.nav_item_title);
				holder = new ViewHolder();
				holder.image = image;
				holder.text = text;
				view.setTag(holder);
			} else {
				image = holder.image;
				text = holder.text;
			}
			image.setImageResource(command.imageResId);
			text.setText(command.title);

			return view;
		}

	}

	private static class ViewHolder {
		ImageView image;
		TextView text;
	}

	private static class CommandItem {
		int imageResId;
		String title;
	}

	private List<CommandItem> createNavCommandItems() {
		List<CommandItem> list = new ArrayList<CommandItem>();
		CommandItem item = new CommandItem();
		item.imageResId = R.drawable.interesting;
		item.title = getActivity().getString(R.string.item_interesting_photo);
		list.add(item);

		item = new CommandItem();
		item.imageResId = R.drawable.photos;
		item.title = getActivity().getString(R.string.item_my_photo);
		list.add(item);

		item = new CommandItem();
		item.imageResId = R.drawable.gallery;
		item.title = getActivity().getString(R.string.item_my_sets);
		list.add(item);

		item = new CommandItem();
		item.imageResId = R.drawable.contacts;
		item.title = getActivity().getString(R.string.item_my_contact);
		list.add(item);

		item = new CommandItem();
		item.imageResId = R.drawable.myfavorite;
		item.title = getActivity().getString(R.string.item_my_fav);
		list.add(item);

		item = new CommandItem();
		item.imageResId = R.drawable.activities;
		item.title = getActivity().getString(R.string.item_recent_activities);
		list.add(item);

		item = new CommandItem();
		item.imageResId = R.drawable.settings;
		item.title = getActivity().getString(R.string.item_settings);
		list.add(item);

		return list;
	}

}
