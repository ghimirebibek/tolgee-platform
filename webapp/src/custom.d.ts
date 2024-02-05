import { PaletteColor } from '@mui/material/styles';
import { PaletteColorOptions } from '@mui/material';
import {
  Activity,
  BillingProgress,
  Cell,
  Editor,
  Emphasis,
  ExampleBanner,
  Marker,
  Navbar,
  QuickStart,
  Tile,
  TipsBanner,
  TopBanner,
} from './colors';

declare module '*.svg' {
  const content: React.FunctionComponent<React.SVGAttributes<SVGElement>>;
  export default content;
}
import { colors } from './colors';

const all = { ...colors.light, ...colors.dark };

declare module '@mui/material/styles/createPalette' {
  interface Palette {
    primaryText: string;
    divider1: string;
    tile: Tile;
    cell: Cell;
    default: PaletteColor;
    navbar: Navbar;
    emphasis: Emphasis;
    activity: Activity;
    editor: Editor;
    billingProgress: BillingProgress;
    billingPlan: PaletteColor;
    globalLoading: PaletteColor;
    marker: Marker;
    topBanner: TopBanner;
    quickStart: QuickStart;
    import: typeof all.import;
    exampleBanner: ExampleBanner;
    tipsBanner: TipsBanner;
  }

  interface PaletteOptions {
    primaryText: string;
    divider1: string;
    tile: Tile;
    cell: Cell;
    default: PaletteColor;
    navbar: Navbar;
    emphasis: Emphasis;
    activity: Activity;
    editor: Editor;
    billingProgress: BillingProgress;
    billingPlan: PaletteColorOptions;
    globalLoading: PaletteColorOptions;
    marker: Marker;
    topBanner: TopBanner;
    quickStart: QuickStart;
    import: typeof all.import;
    exampleBanner: ExampleBanner;
    tipsBanner: TipsBanner;
  }
}

declare module '@mui/material/Button' {
  interface ButtonPropsColorOverrides {
    default: true;
  }
}
