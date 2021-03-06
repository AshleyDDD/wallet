/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mycelium.wallet.lt.activity;

import java.math.BigDecimal;
import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ListView;

import com.google.common.base.Preconditions;
import com.mycelium.lt.api.model.TraderInfo;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.lt.LtAndroidUtils;
import com.mycelium.wallet.lt.activity.TraderInfoAdapter.InfoItem;
import com.mycelium.wallet.lt.api.GetTraderInfo;
import com.mycelium.wallet.lt.api.Request;

public class TraderInfoFragment extends Fragment {

   protected static final int CREATE_TRADER_RESULT_CODE = 0;
   private MbwManager _mbwManager;
   private LocalTraderManager _ltManager;
   private TraderInfoAdapter _adapter;

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View ret = Preconditions.checkNotNull(inflater.inflate(R.layout.lt_trader_info_fragment, container, false));

      ret.findViewById(R.id.btCreate).setOnClickListener(createClickListener);
      return ret;
   }

   OnClickListener createClickListener = new OnClickListener() {

      @Override
      public void onClick(View v) {
         CreateTrader1Activity.callMe(getActivity(), CREATE_TRADER_RESULT_CODE);
      }
   };

   private View findViewById(int id) {
      return getView().findViewById(id);
   }

   @Override
   public void onAttach(Activity activity) {
      _mbwManager = MbwManager.getInstance(getActivity().getApplication());
      _ltManager = _mbwManager.getLocalTraderManager();
      super.onAttach(activity);
   }

   @Override
   public void onDetach() {
      super.onDetach();
   }

   @Override
   public void onResume() {
      _adapter = new TraderInfoAdapter(getActivity(), new ArrayList<TraderInfoAdapter.InfoItem>());
      ListView list = (ListView) findViewById(R.id.lvTraderInfo);
      list.setAdapter(_adapter);
      updateUi();
      _ltManager.subscribe(ltSubscriber);
      super.onResume();
   }

   @Override
   public void onPause() {
      _ltManager.unsubscribe(ltSubscriber);
      super.onPause();
   }

   @Override
   public void onDestroy() {
      super.onDestroy();
   }

   private void updateUi() {
      if (!isAdded()) {
         return;
      }
      TraderInfo info = _ltManager.getCachedTraderInfo();
      if (!_ltManager.hasLocalTraderAccount()) {
         findViewById(R.id.svNoAccount).setVisibility(View.VISIBLE);
         findViewById(R.id.lvTraderInfo).setVisibility(View.GONE);
         findViewById(R.id.pbWait).setVisibility(View.GONE);
      } else if (info == null) {
         findViewById(R.id.svNoAccount).setVisibility(View.GONE);
         findViewById(R.id.lvTraderInfo).setVisibility(View.GONE);
         findViewById(R.id.pbWait).setVisibility(View.VISIBLE);
      } else {
         findViewById(R.id.svNoAccount).setVisibility(View.GONE);
         findViewById(R.id.lvTraderInfo).setVisibility(View.VISIBLE);
         findViewById(R.id.pbWait).setVisibility(View.GONE);
         populateTraderInfo(info);
      }
   }

   private void populateTraderInfo(TraderInfo i) {
      _adapter.clear();
      _adapter.add(new InfoItem(getString(R.string.lt_trader_name_label), i.nickname));
      _adapter.add(new InfoItem(getString(R.string.lt_trader_address_label), i.address.toMultiLineString()));
      _adapter.add(new InfoItem(getString(R.string.lt_trader_age_label), getResources().getString(
            R.string.lt_time_in_days, i.traderAgeMs / Constants.MS_PR_DAY)));
      _adapter.add(new InfoItem(getString(R.string.lt_successful_sells_label), Integer.toString(i.successfulSales)));
      _adapter.add(new InfoItem(getString(R.string.lt_aborted_sells_label), Integer.toString(i.abortedSales)));
      _adapter
            .add(new InfoItem(getString(R.string.lt_total_sold_label), _mbwManager.getBtcValueString(i.totalBtcSold)));
      _adapter.add(new InfoItem(getString(R.string.lt_successful_buys_label), Integer.toString(i.successfulBuys)));
      _adapter.add(new InfoItem(getString(R.string.lt_aborted_buys_label), Integer.toString(i.abortedBuys)));
      _adapter.add(new InfoItem(getString(R.string.lt_total_bought_label), _mbwManager
            .getBtcValueString(i.totalBtcBought)));

      // Rating
      float rating = LtAndroidUtils.calculate5StarRating(i.successfulSales, i.abortedSales, i.successfulBuys,
            i.abortedBuys, i.traderAgeMs);
      _adapter.add(new InfoItem(getString(R.string.lt_rating_label), rating));

      // Median trade time
      if (i.tradeMedianMs != null) {
         String hourString = LtAndroidUtils.getApproximateTimeInHours(getActivity(), i.tradeMedianMs);
         _adapter.add(new InfoItem(getString(R.string.lt_expected_trade_time_label), hourString));
      }

      // Commission
      _adapter.add(new InfoItem(getString(R.string.lt_local_trader_commission_label), roundDoubleHalfUp(
            i.localTraderPremium, 2).toString()
            + "%"));

   }

   private static Double roundDoubleHalfUp(double value, int decimals) {
      return BigDecimal.valueOf(value).setScale(decimals, BigDecimal.ROUND_HALF_UP).doubleValue();
   }

   private LocalTraderEventSubscriber ltSubscriber = new LocalTraderEventSubscriber(new Handler()) {

      @Override
      public void onLtError(int errorCode) {
      }

      public void onLtSendingRequest(Request request) {
         if (request instanceof GetTraderInfo) {
            // Show spinner
            findViewById(R.id.pbWait).setVisibility(View.VISIBLE);
         }
      }

      @Override
      public void onLtTraderInfoFetched(TraderInfo info, GetTraderInfo request) {
         updateUi();
      }
   };

}
