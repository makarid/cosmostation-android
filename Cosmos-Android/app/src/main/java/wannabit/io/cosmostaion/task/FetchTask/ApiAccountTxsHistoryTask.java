package wannabit.io.cosmostaion.task.FetchTask;

import java.util.ArrayList;

import retrofit2.Response;
import wannabit.io.cosmostaion.base.BaseApplication;
import wannabit.io.cosmostaion.base.BaseChain;
import wannabit.io.cosmostaion.base.BaseConstant;
import wannabit.io.cosmostaion.network.ApiClient;
import wannabit.io.cosmostaion.network.res.ResApiTxList;
import wannabit.io.cosmostaion.task.CommonTask;
import wannabit.io.cosmostaion.task.TaskListener;
import wannabit.io.cosmostaion.task.TaskResult;
import wannabit.io.cosmostaion.utils.WLog;

public class ApiAccountTxsHistoryTask extends CommonTask {

    private String mAddress;
    private BaseChain mChain;

    public ApiAccountTxsHistoryTask(BaseApplication app, TaskListener listener, String address, BaseChain chain) {
        super(app, listener);
        this.mResult.taskType   = BaseConstant.TASK_FETCH_API_ADDRESS_HISTORY;
        this.mAddress = address;
        this.mChain = chain;
    }

    @Override
    protected TaskResult doInBackground(String... strings) {
        try {
            if (mChain.equals(BaseChain.COSMOS_MAIN)) {
            } else if (mChain.equals(BaseChain.IRIS_MAIN)) {
            } else if (mChain.equals(BaseChain.BNB_MAIN)) {
            } else if (mChain.equals(BaseChain.KAVA_MAIN)) {
            } else if (mChain.equals(BaseChain.KAVA_TEST)) {
//                Response<ResApiTxList> response = ApiClient.getKavaTestApi(mApp).getAccountTxs(mAddress).execute();
//                if (response.isSuccessful() && response.body() != null) {
//                    mResult.resultData = response.body().data;
//                    mResult.isSuccess = true;
//                } else {
//                    WLog.w("HistoryTask : NOk");
//                }

                Response<ArrayList<ResApiTxList.Data>> response = ApiClient.getKavaTestApi(mApp).getAccountTxs(mAddress).execute();
                if (response.isSuccessful() && response.body() != null) {
                    mResult.resultData = response.body();
                    mResult.isSuccess = true;
                } else {
                    WLog.w("HistoryTask : NOk");
                }

            } else if (mChain.equals(BaseChain.IOV_MAIN)) {

            }

        } catch (Exception e) {
            e.printStackTrace();
            WLog.w("ApiAccountTxsHistoryTask Error " + e.getMessage());
        }
        return mResult;
    }
}
