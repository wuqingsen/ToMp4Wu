package com.wuqingsen.openglrecordmp4wu.utils.helper;


import com.wuqingsen.openglrecordmp4wu.record.basefilter.GPUImageFilter;
import com.wuqingsen.openglrecordmp4wu.utils.filter.MagicAntiqueFilter;
import com.wuqingsen.openglrecordmp4wu.utils.filter.MagicBrannanFilter;
import com.wuqingsen.openglrecordmp4wu.utils.filter.MagicCoolFilter;
import com.wuqingsen.openglrecordmp4wu.utils.filter.MagicFreudFilter;
import com.wuqingsen.openglrecordmp4wu.utils.filter.MagicHefeFilter;
import com.wuqingsen.openglrecordmp4wu.utils.filter.MagicHudsonFilter;
import com.wuqingsen.openglrecordmp4wu.utils.filter.MagicInkwellFilter;
import com.wuqingsen.openglrecordmp4wu.utils.filter.MagicN1977Filter;
import com.wuqingsen.openglrecordmp4wu.utils.filter.MagicNashvilleFilter;

public class MagicFilterFactory {

    private static MagicFilterType filterType = MagicFilterType.NONE;

    public static GPUImageFilter initFilters(MagicFilterType type) {
        if (type == null) {
            return null;
        }
        filterType = type;
        switch (type) {
            case ANTIQUE:
                return new MagicAntiqueFilter();
            case BRANNAN:
                return new MagicBrannanFilter();
            case FREUD:
                return new MagicFreudFilter();
            case HEFE:
                return new MagicHefeFilter();
            case HUDSON:
                return new MagicHudsonFilter();
            case INKWELL:
                return new MagicInkwellFilter();
            case N1977:
                return new MagicN1977Filter();
            case NASHVILLE:
                return new MagicNashvilleFilter();
            case COOL:
                return new MagicCoolFilter();
            case WARM:
                return new MagicWarmFilter();
            default:
                return null;
        }
    }

    public MagicFilterType getCurrentFilterType() {
        return filterType;
    }

    private static class MagicWarmFilter extends GPUImageFilter {
    }
}
