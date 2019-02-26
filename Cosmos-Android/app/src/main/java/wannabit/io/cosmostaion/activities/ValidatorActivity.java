package wannabit.io.cosmostaion.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.math.BigDecimal;
import java.util.ArrayList;

import agency.tango.android.avatarview.IImageLoader;
import agency.tango.android.avatarview.loader.PicassoLoader;
import agency.tango.android.avatarview.views.AvatarView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import wannabit.io.cosmostaion.R;
import wannabit.io.cosmostaion.base.BaseActivity;
import wannabit.io.cosmostaion.base.BaseConstant;
import wannabit.io.cosmostaion.dao.Account;
import wannabit.io.cosmostaion.dao.BondingState;
import wannabit.io.cosmostaion.dao.Reward;
import wannabit.io.cosmostaion.dao.UnBondingState;
import wannabit.io.cosmostaion.model.type.Validator;
import wannabit.io.cosmostaion.network.ApiClient;
import wannabit.io.cosmostaion.network.req.ReqTx;
import wannabit.io.cosmostaion.network.req.ReqTxVal;
import wannabit.io.cosmostaion.network.res.ResHistory;
import wannabit.io.cosmostaion.network.res.ResKeyBaseUser;
import wannabit.io.cosmostaion.network.res.ResLcdBondings;
import wannabit.io.cosmostaion.task.FetchTask.HistoryTask;
import wannabit.io.cosmostaion.task.FetchTask.ValHistoryTask;
import wannabit.io.cosmostaion.task.SingleFetchTask.SingleBondingStateTask;
import wannabit.io.cosmostaion.task.SingleFetchTask.SingleRewardTask;
import wannabit.io.cosmostaion.task.SingleFetchTask.SingleSelfBondingStateTask;
import wannabit.io.cosmostaion.task.SingleFetchTask.SingleUnBondingStateTask;
import wannabit.io.cosmostaion.task.SingleFetchTask.SingleValidatorInfoTask;
import wannabit.io.cosmostaion.task.TaskListener;
import wannabit.io.cosmostaion.task.TaskResult;
import wannabit.io.cosmostaion.utils.WDp;
import wannabit.io.cosmostaion.utils.WKey;
import wannabit.io.cosmostaion.utils.WLog;

public class ValidatorActivity extends BaseActivity implements TaskListener {

    private Toolbar                     mToolbar;
    private TextView                    mToolbarNickName;
    private TextView                    mToolbarAddress;
    private SwipeRefreshLayout          mSwipeRefreshLayout;
    private RecyclerView                mRecyclerView;
    private LinearLayoutManager         mLinearLayoutManager;

    private ValidatorAdapter            mValidatorAdapter;

    private Account                     mAccount;
    private Validator                   mValidator;
    private BondingState                mBondingState;
    private ArrayList<UnBondingState>   mUnBondingStates;
    private Reward                      mReward;
    private ArrayList<ResHistory.InnerHits> mTx = new ArrayList<>();

    private String                      mValidatorPicture;
    private String                      mSelfBondingRate;

    private int                         mTaskCount;
    private boolean                     mExpended = true;

    private IImageLoader                mImageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_validator);
        mToolbar                    = findViewById(R.id.tool_bar);
        mToolbarNickName            = findViewById(R.id.toolbar_nickName);
        mToolbarAddress             = findViewById(R.id.toolbar_Address);
        mSwipeRefreshLayout         = findViewById(R.id.layer_refresher);
        mRecyclerView               = findViewById(R.id.recycler);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mImageLoader = new PicassoLoader();

        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorPrimary));
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
//                onFetchAccounts();
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
        mLinearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.setHasFixedSize(true);

        WLog.w("Address : " + getIntent().getStringExtra("valAddr"));


    }

    @Override
    protected void onResume() {
        super.onResume();

        mAccount = getBaseDao().onSelectAccount(getBaseDao().getLastUser());
        if(mAccount == null) {
            WLog.w("NO Account ERROR");
        }

        if(getBaseDao().getValidator() == null) {
            WLog.w("NO Validator ERROR");
        }

        mValidator          = getBaseDao().getValidator();
        mBondingState       = getBaseDao().onSelectBondingState(mAccount.id, mValidator.operator_address);
        mUnBondingStates    = getBaseDao().onSelectUnbondingStates(mAccount.id, mValidator.operator_address);
//        mReward         = getBaseDao().getValidatorDetail().mReward;

        if(TextUtils.isEmpty(mAccount.nickName)) {
            mToolbarNickName.setText("Wallet " + mAccount.id);
        } else {
            mToolbarNickName.setText(mAccount.nickName);
        }
        mToolbarAddress.setText(mAccount.address);


        WLog.w("Validator : " + mValidator.description.moniker);
        WLog.w("Validator : " + mValidator.operator_address);
        mValidatorAdapter = new ValidatorAdapter(mImageLoader);
        mRecyclerView.setAdapter(mValidatorAdapter);

        onInitFetch();
        onFetchValHistory();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void onInitFetch() {
        if(mTaskCount > 0) return;
        mTaskCount = 4;
        new SingleValidatorInfoTask(getBaseApplication(), this, mValidator.operator_address).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        new SingleBondingStateTask(getBaseApplication(), this, mAccount, mValidator.operator_address).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        new SingleSelfBondingStateTask(getBaseApplication(), this, WKey.convertDpOpAddressToDpAddress(mValidator.operator_address), mValidator.operator_address).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        new SingleUnBondingStateTask(getBaseApplication(), this, mAccount, mValidator.operator_address).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        if(mBondingState != null) {
            mTaskCount = mTaskCount + 1;
            new SingleRewardTask(getBaseApplication(), this, mAccount, mValidator.operator_address).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

    }

    private void onFetchValHistory() {
        WLog.w("onFetchValHistory");
        mTaskCount++;
        ReqTxVal req = new ReqTxVal(0, 0, true, mAccount.address, mValidator.operator_address);
//        String jsonText = new Gson().toJson(req);
//        WLog.w("jsonText : " + jsonText);
        new ValHistoryTask(getBaseApplication(), this, req).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onTaskResponse(TaskResult result) {
//        WLog.w("TaskResult : " + result.taskType);
        mTaskCount--;
        if (result.taskType == BaseConstant.TASK_FETCH_SINGLE_VALIDATOR) {
            mValidator = (Validator)result.resultData;
            if(mValidator == null) {
                WLog.w("Show network error!!!");
            }

        } else if (result.taskType == BaseConstant.TASK_FETCH_SINGLE_BONDING) {
            mBondingState   = getBaseDao().onSelectBondingState(mAccount.id, mValidator.operator_address);
            if(mBondingState != null && mValidator != null) {
                mTaskCount = mTaskCount + 1;
                new SingleRewardTask(getBaseApplication(), this, mAccount, mValidator.operator_address).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

        } else if (result.taskType == BaseConstant.TASK_FETCH_SINGLE_UNBONDING) {
            mUnBondingStates = getBaseDao().onSelectUnbondingStates(mAccount.id, mValidator.operator_address);
            WLog.w("mUnBondingStates : " + mUnBondingStates.size());

        } else if (result.taskType == BaseConstant.TASK_FETCH_SINGLE_SELF_BONDING) {
            ResLcdBondings temp = (ResLcdBondings)result.resultData;
            if(temp != null)
                mSelfBondingRate = WDp.getSelfBondRate(mValidator.tokens, temp.shares);


        } else if (result.taskType == BaseConstant.TASK_FETCH_SINGLE_REWARD) {
            mReward         = (Reward)result.resultData;

        } else if (result.taskType == BaseConstant.TASK_FETCH_VAL_HISTORY) {
            ArrayList<ResHistory.InnerHits> hits = (ArrayList<ResHistory.InnerHits>)result.resultData;
            if(hits != null && hits.size() > 0) {
                WLog.w("hit size " + hits.size());
                mTx = hits;
            } else {
                WLog.w("hit null");
            }
        }



        if(mTaskCount == 0) {
            WLog.w("mTaskFinished");
            mValidatorAdapter.notifyDataSetChanged();
        }
    }


    private class ValidatorAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_VALIDATOR             = 0;
        private static final int TYPE_MY_VALIDATOR          = 1;
        private static final int TYPE_ACTION                = 2;
        private static final int TYPE_HISTORY_HEADER        = 3;
        private static final int TYPE_HISTORY               = 4;
        private static final int TYPE_HISTORY_EMPTY         = 5;
        IImageLoader    mImageLoader;

        public ValidatorAdapter(IImageLoader mImageLoader) {
            this.mImageLoader = mImageLoader;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
            if(viewType == TYPE_VALIDATOR) {
                return new ValidatorHolder(getLayoutInflater().inflate(R.layout.item_validator_detail, viewGroup, false));
            } else if(viewType == TYPE_MY_VALIDATOR) {
                return new MyValidatorHolder(getLayoutInflater().inflate(R.layout.item_validator_my_detail, viewGroup, false));
            } else if(viewType == TYPE_ACTION) {
                return new MyActionHolder(getLayoutInflater().inflate(R.layout.item_validator_my_action, viewGroup, false));
            } else if(viewType == TYPE_HISTORY_HEADER) {
                return new HistoryHeaderHolder(getLayoutInflater().inflate(R.layout.item_validator_history_header, viewGroup, false));
            } else if(viewType == TYPE_HISTORY) {
                return new HistoryHolder(getLayoutInflater().inflate(R.layout.item_validator_history, viewGroup, false));
            } else if(viewType == TYPE_HISTORY_EMPTY) {
                return new HistoryEmptyHolder(getLayoutInflater().inflate(R.layout.item_validator_history_empty, viewGroup, false));
            }
            return null;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
            if (getItemViewType(position) == TYPE_VALIDATOR) {
                final ValidatorHolder holder = (ValidatorHolder)viewHolder;
                holder.itemTvMoniker.setText(mValidator.description.moniker);
                holder.itemTvAddress.setText(mValidator.operator_address);
                if(!TextUtils.isEmpty(mValidator.description.website)) holder.itemTvWebsite.setText(mValidator.description.website);
                else holder.itemTvWebsite.setVisibility(View.GONE);
                if(!TextUtils.isEmpty(mValidator.description.details)) holder.itemTvDescription.setText(mValidator.description.details);
                else holder.itemTvDescription.setVisibility(View.GONE);

                mImageLoader.loadImage(holder.itemAvatar, "error", mValidator.description.moniker);
                if(!TextUtils.isEmpty(mValidator.description.identity)) {
                    ApiClient.getKeybaseService(getBaseContext()).getUserInfo("pictures", mValidator.description.identity).enqueue(new Callback<ResKeyBaseUser>() {
                        @Override
                        public void onResponse(Call<ResKeyBaseUser> call, Response<ResKeyBaseUser> response) {
                            mImageLoader.loadImage(holder.itemAvatar, response.body().getUrl(), mValidator.description.moniker);
                        }
                        @Override
                        public void onFailure(Call<ResKeyBaseUser> call, Throwable t) { }
                    });
                }

                holder.itemTvTotalBondAmount.setText(WDp.getDpAmount(getBaseContext(), new BigDecimal(mValidator.tokens), 6));
                holder.itemTvCommissionRate.setText(WDp.getCommissionRate(mValidator.commission.rate));
                if(!TextUtils.isEmpty(mSelfBondingRate)) holder.itemTvSelfBondRate.setText(mSelfBondingRate); else holder.itemTvSelfBondRate.setText("");
                holder.itemBtnDelegate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        WLog.w("Start Delegate");
                        getBaseDao().setValidator(mValidator);
                        Intent toDelegate = new Intent(ValidatorActivity.this, DelegateActivity.class);
                        startActivity(toDelegate);
                    }
                });


            } else if (getItemViewType(position) == TYPE_MY_VALIDATOR) {
                final MyValidatorHolder holder = (MyValidatorHolder)viewHolder;
                holder.itemTvMoniker.setText(mValidator.description.moniker);
                holder.itemTvAddress.setText(mValidator.operator_address);
                if(!TextUtils.isEmpty(mValidator.description.website)) holder.itemTvWebsite.setText(mValidator.description.website);
                else holder.itemTvWebsite.setVisibility(View.GONE);
                if(!TextUtils.isEmpty(mValidator.description.details)) holder.itemTvDescription.setText(mValidator.description.details);
                else holder.itemTvDescription.setVisibility(View.GONE);

                mImageLoader.loadImage(holder.itemAvatar, "error", mValidator.description.moniker);
                if(!TextUtils.isEmpty(mValidator.description.identity)) {
                    ApiClient.getKeybaseService(getBaseContext()).getUserInfo("pictures", mValidator.description.identity).enqueue(new Callback<ResKeyBaseUser>() {
                        @Override
                        public void onResponse(Call<ResKeyBaseUser> call, Response<ResKeyBaseUser> response) {
                            mImageLoader.loadImage(holder.itemAvatar, response.body().getUrl(), mValidator.description.moniker);
                        }
                        @Override
                        public void onFailure(Call<ResKeyBaseUser> call, Throwable t) { }
                    });
                }

                holder.itemHideShow.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        WLog.w("TOGGLE");
                    }
                });

                holder.itemTvTotalBondAmount.setText(WDp.getDpAmount(getBaseContext(), new BigDecimal(mValidator.tokens), 6));
                holder.itemTvCommissionRate.setText(WDp.getCommissionRate(mValidator.commission.rate));
                if(!TextUtils.isEmpty(mSelfBondingRate)) holder.itemTvSelfBondRate.setText(mSelfBondingRate); else holder.itemTvSelfBondRate.setText("");

            } else if (getItemViewType(position) == TYPE_ACTION) {
                final MyActionHolder holder = (MyActionHolder)viewHolder;
                holder.itemAtomTitle.setText(WDp.DpAtom(getBaseContext(), mAccount.baseChain));
                holder.itemPhotonTitle.setText(WDp.DpPoton(getBaseContext(), mAccount.baseChain));
                if(mBondingState != null && mBondingState.shares != null) {
                    holder.itemTvDelegatedAmount.setText(WDp.getDpAmount(getBaseContext(), mBondingState.shares, 6));
                } else {
                    holder.itemTvDelegatedAmount.setText(WDp.getDpAmount(getBaseContext(), BigDecimal.ZERO, 0));
                }

                if(mUnBondingStates != null && mUnBondingStates.size() > 0 ) {
                    BigDecimal sum = BigDecimal.ZERO;
                    for(UnBondingState unbond:mUnBondingStates) {
                        sum = sum.add(unbond.balance);
                    }
                    WLog.w("sum" + sum.toPlainString());
                    holder.itemTvUnbondingAmount.setText(WDp.getDpAmount(getBaseContext(), sum, 6));
//                    holder.itemTvUnbondingTime.setText(WDp.getUnbondingTimeleft(getBaseContext(), mUnBondingState.completionTime));
//                    holder.itemTvUnbondingTime.setVisibility(View.VISIBLE);
                    holder.itemTvUnbondingTime.setVisibility(View.INVISIBLE);
                } else {
                    holder.itemTvUnbondingAmount.setText(WDp.getDpAmount(getBaseContext(), BigDecimal.ZERO, 6));
                    holder.itemTvUnbondingTime.setVisibility(View.INVISIBLE);
                }
                if(mReward != null) {
                    holder.itemTvAtomReward.setText(WDp.getDpAmount(getBaseContext(), mReward.getAtomAmount(), 6));
                    holder.itemTvPhotonReward.setText(WDp.getDpAmount(getBaseContext(), mReward.getPhotonAmount(), 6));
                } else {
                    holder.itemTvAtomReward.setText(WDp.getDpAmount(getBaseContext(), BigDecimal.ZERO, 6));
                    holder.itemTvPhotonReward.setText(WDp.getDpAmount(getBaseContext(), BigDecimal.ZERO, 6));
                }
                holder.itemBtnDelegate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        WLog.w("Start Delegate");
                        getBaseDao().setValidator(mValidator);
                        Intent toDelegate = new Intent(ValidatorActivity.this, DelegateActivity.class);
                        startActivity(toDelegate);
                    }
                });
                holder.itemBtnUndelegate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        WLog.w("Start Undelegate");
                        if(mBondingState == null || mBondingState.shares.compareTo(BigDecimal.ZERO) <= 0) {
                            Toast.makeText(getBaseContext(), R.string.error_no_delegate, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        getBaseDao().setValidator(mValidator);
                        Intent unDelegate = new Intent(ValidatorActivity.this, UndelegateActivity.class);
                        startActivity(unDelegate);

                    }
                });
                holder.itemBtnReward.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        WLog.w("Start Reward");
                        if(mBondingState == null || mBondingState.shares.compareTo(BigDecimal.ZERO) <= 0) {
                            Toast.makeText(getBaseContext(), R.string.error_no_delegate, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        getBaseDao().setValidator(mValidator);
                        Intent claimReward = new Intent(ValidatorActivity.this, ClaimRewardActivity.class);
                        claimReward.putExtra("isAll", false);
                        startActivity(claimReward);

                    }
                });

            } else if (getItemViewType(position) == TYPE_HISTORY_HEADER) {
                final HistoryHeaderHolder holder = (HistoryHeaderHolder)viewHolder;

            } else if (getItemViewType(position) == TYPE_HISTORY) {
                final HistoryHolder holder = (HistoryHolder)viewHolder;
                final ResHistory.Source source;
                if(mBondingState == null && (mUnBondingStates == null || mUnBondingStates.size() < 1)) {
                    source = mTx.get(position - 2)._source;
                } else {
                    source = mTx.get(position - 3)._source;
                }

                holder.history_time.setText(WDp.getTimeformat(getBaseContext(), source.time));

                int dpType = WDp.getHistoryDpType(source.tx.value.msg, mAccount.address);
                switch (dpType) {
                    case BaseConstant.TX_TYPE_SEND:
                        holder.historyType.setText(getString(R.string.tx_send));
                        holder.historyType.setTextColor(getResources().getColor(R.color.colorPhoton));
                        break;

                    case BaseConstant.TX_TYPE_RECEIVE:
                        holder.historyType.setText(getString(R.string.tx_receive));
                        holder.historyType.setTextColor(getResources().getColor(R.color.colorAtom));
                        break;

                    case BaseConstant.TX_TYPE_TRANSFER:
                        holder.historyType.setText(getString(R.string.tx_transfer));
                        holder.historyType.setTextColor(getResources().getColor(R.color.colorWhite));
                        break;

                    case BaseConstant.TX_TYPE_DELEGATE:
                        holder.historyType.setText(getString(R.string.tx_delegate));
                        holder.historyType.setTextColor(getResources().getColor(R.color.colorWhite));
                        break;

                    case BaseConstant.TX_TYPE_UNDELEGATE:
                        holder.historyType.setText(getString(R.string.tx_undelegate));
                        holder.historyType.setTextColor(getResources().getColor(R.color.colorWhite));
                        holder.history_time.setText(WDp.getTimeformat(getBaseContext(), source.time) + " " +
                                WDp.getUnbondingTimefrom(getBaseContext(), source.time));
                        break;

                    case BaseConstant.TX_TYPE_REDELEGATE:
                        holder.historyType.setText(getString(R.string.tx_redelegate));
                        holder.historyType.setTextColor(getResources().getColor(R.color.colorWhite));
                        break;

                    case BaseConstant.TX_TYPE_GET_REWARD:
                        holder.historyType.setText(getString(R.string.tx_get_reward));
                        holder.historyType.setTextColor(getResources().getColor(R.color.colorWhite));
                        break;

                    case BaseConstant.TX_TYPE_GET_CPMMISSION:
                        holder.historyType.setText(getString(R.string.tx_get_commission));
                        holder.historyType.setTextColor(getResources().getColor(R.color.colorWhite));
                        break;

                    case BaseConstant.TX_TYPE_CHAGE_REWARD_ADDRESS:
                        holder.historyType.setText(getString(R.string.tx_change_reward_address));
                        holder.historyType.setTextColor(getResources().getColor(R.color.colorWhite));
                        break;

                    case BaseConstant.TX_TYPE_UNKNOWN:
                        holder.historyType.setText(getString(R.string.tx_known));
                        holder.historyType.setTextColor(getResources().getColor(R.color.colorWhite));
                        break;

                }

                if(!source.result.isSuccess()) {
                    holder.historySuccess.setVisibility(View.VISIBLE);
                } else {
                    holder.historySuccess.setVisibility(View.GONE);
                }


                holder.history_block.setText(source.height + " block");
                holder.history_hash.setText(source.hash);
                holder.historyRoot.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        WLog.w("GO detail!!");
                        Intent webintent = new Intent(getBaseContext(), WebActivity.class);
                        webintent.putExtra("txid", source.hash);
                        startActivity(webintent);
                    }
                });


            } else if (getItemViewType(position) == TYPE_HISTORY_EMPTY) {
                final HistoryEmptyHolder holder = (HistoryEmptyHolder)viewHolder;

            }
        }

        @Override
        public int getItemViewType(int position) {
            if(mBondingState == null && (mUnBondingStates == null || mUnBondingStates.size() < 1)) {
                if(position == 0) {
                    return TYPE_VALIDATOR;
                } else if (position == 1) {
                    return TYPE_HISTORY_HEADER;
                }

            } else {
                if(position == 0) {
                    return TYPE_MY_VALIDATOR;
                } else if (position == 1) {
                    return TYPE_ACTION;
                } else if (position == 2) {
                    return TYPE_HISTORY_HEADER;
                }
            }
            if(mTx.size() > 0) {
                return TYPE_HISTORY;
            }
            return TYPE_HISTORY_EMPTY;
        }


        @Override
        public int getItemCount() {
            if(mBondingState == null && (mUnBondingStates == null || mUnBondingStates.size() < 1)) {
                if(mTx.size() > 0) {
                    return mTx.size() + 2;
                } else {
                    return 3;
                }
            } else {
                if(mTx.size() > 0) {
                    return mTx.size() + 3;
                } else {
                    return 4;
                }
            }
        }



        public class ValidatorHolder extends RecyclerView.ViewHolder {
            AvatarView  itemAvatar;
            TextView    itemTvMoniker;
            TextView    itemTvAddress;
            TextView    itemTvWebsite;
            TextView    itemTvDescription;
            TextView    itemTvTotalBondAmount;
            TextView    itemTvSelfBondRate;
            TextView    itemTvCommissionRate;
            Button      itemBtnDelegate;

            public ValidatorHolder(View v) {
                super(v);
                itemAvatar              = itemView.findViewById(R.id.validator_avatar);
                itemTvMoniker           = itemView.findViewById(R.id.validator_moniker);
                itemTvAddress           = itemView.findViewById(R.id.validator_address);
                itemTvWebsite           = itemView.findViewById(R.id.validator_site);
                itemTvDescription       = itemView.findViewById(R.id.validator_description);
                itemTvTotalBondAmount   = itemView.findViewById(R.id.validator_total_bonded);
                itemTvSelfBondRate      = itemView.findViewById(R.id.validator_self_bond_rate);
                itemTvCommissionRate    = itemView.findViewById(R.id.validator_commission);
                itemBtnDelegate         = itemView.findViewById(R.id.validator_btn_delegate);
            }
        }

        public class MyValidatorHolder extends RecyclerView.ViewHolder {
            AvatarView  itemAvatar;
            ImageView   itemHideShow;
            TextView    itemTvMoniker;
            TextView    itemTvAddress;
            TextView    itemTvWebsite;
            TextView    itemTvDescription;
            TextView    itemTvTotalBondAmount;
            TextView    itemTvSelfBondRate;
            TextView    itemTvCommissionRate;

            public MyValidatorHolder(View v) {
                super(v);
                itemAvatar              = itemView.findViewById(R.id.validator_avatar);
                itemHideShow            = itemView.findViewById(R.id.validator_hide_show);
                itemTvMoniker           = itemView.findViewById(R.id.validator_moniker);
                itemTvAddress           = itemView.findViewById(R.id.validator_address);
                itemTvWebsite           = itemView.findViewById(R.id.validator_site);
                itemTvDescription       = itemView.findViewById(R.id.validator_description);
                itemTvTotalBondAmount   = itemView.findViewById(R.id.validator_total_bonded);
                itemTvSelfBondRate      = itemView.findViewById(R.id.validator_self_bond_rate);
                itemTvCommissionRate    = itemView.findViewById(R.id.validator_commission);
            }
        }

        public class MyActionHolder extends RecyclerView.ViewHolder {
            TextView    itemTvDelegatedAmount, itemTvUnbondingAmount, itemTvUnbondingTime, itemTvAtomReward, itemTvPhotonReward;
            Button      itemBtnDelegate, itemBtnUndelegate, itemBtnReward;
            TextView    itemAtomTitle, itemPhotonTitle;

            public MyActionHolder(View v) {
                super(v);
                itemTvDelegatedAmount   = itemView.findViewById(R.id.validator_delegated);
                itemTvUnbondingAmount   = itemView.findViewById(R.id.validator_unbonding);
                itemTvUnbondingTime     = itemView.findViewById(R.id.validator_unbonding_time);
                itemTvAtomReward        = itemView.findViewById(R.id.validator_atom_reward);
                itemTvPhotonReward      = itemView.findViewById(R.id.validator_photon_reward);
                itemBtnDelegate         = itemView.findViewById(R.id.validator_btn_delegate);
                itemBtnUndelegate       = itemView.findViewById(R.id.validator_btn_undelegate);
                itemBtnReward           = itemView.findViewById(R.id.validator_btn_claim_reward);
                itemAtomTitle           = itemView.findViewById(R.id.action_atom_title);
                itemPhotonTitle         = itemView.findViewById(R.id.action_photon_title);
            }
        }

        public class HistoryHeaderHolder extends RecyclerView.ViewHolder {

            public HistoryHeaderHolder(View v) {
                super(v);
            }
        }


        public class HistoryHolder extends RecyclerView.ViewHolder {
            private CardView historyRoot;
            private TextView historyType, historySuccess, history_time, history_block, history_hash;

            public HistoryHolder(View v) {
                super(v);
                historyRoot         = itemView.findViewById(R.id.card_history);
                historyType         = itemView.findViewById(R.id.history_type);
                historySuccess      = itemView.findViewById(R.id.history_success);
                history_time        = itemView.findViewById(R.id.history_time);
                history_block       = itemView.findViewById(R.id.history_block_height);
                history_hash        = itemView.findViewById(R.id.history_hash);
            }
        }

        public class HistoryEmptyHolder extends RecyclerView.ViewHolder {

            public HistoryEmptyHolder(View v) {
                super(v);
            }
        }

    }

}
