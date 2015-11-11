package Events;

/**
 * Created by gjacobs on 11/11/15.
 */
public class ForecastListSelected {
    int selectedItem;

    public ForecastListSelected(int selectedItem) {
        this.selectedItem = selectedItem;
    }

    public int getSelectedItem() {
        return selectedItem;
    }

}
