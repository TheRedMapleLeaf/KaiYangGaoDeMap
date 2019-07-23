package org.zarroboogs.maps.ui.navi;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMap.OnMapLoadedListener;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.AMapNaviViewOptions;
import com.amap.api.navi.model.AMapNaviPath;
import com.amap.api.navi.model.NaviLatLng;
import com.amap.api.navi.view.RouteOverLay;

import org.zarroboogs.maps.beans.BJCamera;
import org.zarroboogs.maps.ui.BaseActivity;
import org.zarroboogs.maps.ui.anim.AnimEndListener;
import org.zarroboogs.maps.ui.anim.ViewAnimUtils;
import org.zarroboogs.maps.ui.maps.MapsMainActivity;
import org.zarroboogs.maps.R;
import org.zarroboogs.maps.module.TTSController;
import org.zarroboogs.maps.presenters.MarkerInteractor;
import org.zarroboogs.maps.presenters.MarkerInteractorImpl;
import org.zarroboogs.maps.utils.SettingUtils;
import org.zarroboogs.maps.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 路径规划结果展示界面
 */
public class NaviRouteActivity extends BaseActivity implements OnClickListener,
		OnMapLoadedListener{

	// View
	private ImageButton mStartNaviButton;// 实时导航按钮
	private MapView mMapView;// 地图控件
	private ImageView mRouteBackView;// 返回按钮
	private TextView mRouteDistanceView;// 距离显示控件
	private TextView mRouteTimeView;// 时间显示控件
	private TextView mRouteCostView;// 花费显示控件
	private ListView mRoutSettingListView;
    private TextView mShowRoutType;


	// 地图导航资源
	private AMap mAmap;
	private AMapNavi mAmapNavi;
	private RouteOverLay mRouteOverLay;

	private boolean mIsMapLoaded = false;

	private List<NaviLatLng> mEndNavi;
	private List<NaviLatLng> mStartNavi;

	public static final String NAVI_ENDS = "navi_ends";
	public static final String NAVI_START = "navi_start";

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_route);

		mEndNavi = getIntent().getParcelableArrayListExtra(NAVI_ENDS);
		mStartNavi = getIntent().getParcelableArrayListExtra(NAVI_START);

		Log.d("NaviRouteActivity ", "onCreate- " + mEndNavi.get(0).getLatitude());

		initView(savedInstanceState);


		mAmapNavi = AMapNavi.getInstance(this);
		mAmapNavi.setAMapNaviListener(naviRouteListener);
		boolean startGps = mAmapNavi.startGPS();
		Log.d("NaviRouteActivity ", "onCreate- startGps- " + startGps);

		boolean iscalDrive = mAmapNavi.calculateDriveRoute(mStartNavi, mEndNavi, null, AMapNavi.DrivingDefault);

		Log.d("NaviRouteActivity ", "onCreate- calculateDriveRoute- " + iscalDrive);


		if (SettingUtils.readCurrentCameraState() == SettingUtils.SWITCH_ON){
			MarkerInteractor markerInteractor = new MarkerInteractorImpl();
			markerInteractor.readCameras(new MarkerInteractor.OnReadCamerasListener() {
				BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.icon_camera_location);
				@Override
				public void onReadCameras(ArrayList<BJCamera> cameraBeans) {
					ArrayList<MarkerOptions> markerOptionses = new ArrayList<>();
					for (BJCamera cameraBean : cameraBeans) {
						LatLng latLng = new LatLng(cameraBean.getLatitude(), cameraBean.getLongtitude());
						MarkerOptions mo = new MarkerOptions().position(latLng).draggable(true).icon(icon);
						markerOptionses.add(mo);
					}

					mAmap.addMarkers(markerOptionses,false);
				}
			});
		}

	}

	private  NaviRouteListener naviRouteListener = new NaviRouteListener() {
		@Override
		public void onRouteSuccess() {
			TTSController.getInstance(getApplicationContext()).playText("路线规划成功");
			initNavi();
		}

		@Override
		public void onRouteFailed() {
			TTSController.getInstance(getApplicationContext()).playText("规划失败");
		}
	};

	// -----------------------初始化----------------------------------

	/**
	 * 初始化控件
	 */
	private void initView(Bundle savedInstanceState) {
		mStartNaviButton = (ImageButton) findViewById(R.id.routestartnavi);

		mRouteBackView = (ImageView) findViewById(R.id.route_back_view);


		mRouteDistanceView = (TextView) findViewById(R.id.navi_route_distance);
		mRouteTimeView = (TextView) findViewById(R.id.navi_route_time);
		mRouteCostView = (TextView) findViewById(R.id.navi_route_cost);
		mMapView = (MapView) findViewById(R.id.routemap);
		mMapView.onCreate(savedInstanceState);
		mAmap = mMapView.getMap();
		mAmap.setOnMapLoadedListener(this);
		mStartNaviButton.setOnClickListener(this);
		mRouteBackView.setOnClickListener(this);
		mRouteOverLay = new RouteOverLay(mAmap, null);

        mShowRoutType = (TextView) findViewById(R.id.show_route_tpe);
        mShowRoutType.setText("速度优先");

		mRoutSettingListView = (ListView) findViewById(R.id.search_setting_list_view);
		mRoutSettingListView.setAdapter(new RoutSettingAdapter(getApplicationContext()));

		mRoutSettingListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RoutSettingAdapter adapter = (RoutSettingAdapter) parent.getAdapter();
                RoutType routType = (RoutType) adapter.getItem(position);

                mShowRoutType.setText(routType.getName());


                ViewAnimUtils.pushOutWithInterpolator(mRoutSettingListView, new AnimEndListener() {
                    @Override
                    public void onAnimEnd() {
                        mRoutSettingListView.setVisibility(View.GONE);
                    }
                });
                mAmapNavi.calculateDriveRoute(mStartNavi, mEndNavi, null, routType.getType());
            }
        });

        ImageButton settingBtn = (ImageButton) findViewById(R.id.route_setting);
        settingBtn.setOnClickListener(this);


	}

	/**
	 * 初始化路线描述信息和加载线路
	 */
	private void initNavi() {
		
		AMapNaviPath naviPath = mAmapNavi.getNaviPath();
		if (naviPath == null) {
			return;
		}
		// 获取路径规划线路，显示到地图上
		mRouteOverLay.setRouteInfo(naviPath);
		mRouteOverLay.addToMap();
		if (mIsMapLoaded) {
			mRouteOverLay.zoomToSpan();
		}

		double length = ((int) (naviPath.getAllLength() / (double) 1000 * 10))
				/ (double) 10;
		// 不足分钟 按分钟计
		int time = (naviPath.getAllTime() + 59) / 60;
		int cost = naviPath.getTollCost();
		mRouteDistanceView.setText(String.valueOf(length));
		mRouteTimeView.setText(String.valueOf(time));
		mRouteCostView.setText(String.valueOf(cost));
	}

	private int getThemeStyle() {
		 return AMapNaviViewOptions.BLUE_COLOR_TOPIC;
	}

	// ------------------------------事件处理-----------------------------
	@Override
	public void onClick(View v) {
        switch (v.getId()) {
            // 实时导航操作
            case R.id.routestartnavi: {
                Bundle bundle = new Bundle();
                bundle.putInt(Utils.THEME, getThemeStyle());
                Intent routeIntent = new Intent(NaviRouteActivity.this,
                        NaviCustomActivity.class);
                routeIntent.putExtras(bundle);
                startActivity(routeIntent);
                break;
            }
            // 返回操作
            case R.id.route_back_view: {
                Intent startIntent = new Intent(NaviRouteActivity.this,
                        MapsMainActivity.class);
                startIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(startIntent);
                finish();
                break;
            }
            case R.id.route_setting: {
                if (mRoutSettingListView.getVisibility() == View.VISIBLE){
                    ViewAnimUtils.pushOutWithInterpolator(mRoutSettingListView, new AnimEndListener() {
                        @Override
                        public void onAnimEnd() {
                            mRoutSettingListView.setVisibility(View.GONE);
                        }
                    });
                } else{
                    ViewAnimUtils.dropDownWithInterpolator(mRoutSettingListView, new AnimEndListener() {
                        @Override
                        public void onAnimEnd() {
                            mRoutSettingListView.setVisibility(View.VISIBLE);
                        }
                    });
                }

                break;
            }
        }

    }

	/**
	 * 
	 * 返回键监听
	 * */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent intent = new Intent(NaviRouteActivity.this,
					MapsMainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(intent);
//			MainApplication.getInstance().deleteActivity(this);
			finish();

		}
		return super.onKeyDown(keyCode, event);
	}

	// ------------------------------生命周期必须重写方法---------------------------
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mMapView.onSaveInstanceState(outState);
	}

	@Override
	public void onResume() {
		super.onResume();
		mMapView.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		mMapView.onPause();
	}

	@Override
	public void onDestroy() {
	super.onDestroy();
	mMapView.onDestroy();
}

	@Override
	public void onMapLoaded() {
		mIsMapLoaded = true;
		if (mRouteOverLay != null) {
			mRouteOverLay.zoomToSpan();

		}
	}

}
