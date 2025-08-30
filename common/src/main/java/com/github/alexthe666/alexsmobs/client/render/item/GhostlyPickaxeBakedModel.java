package com.github.alexthe666.alexsmobs.client.render.item;

//
//public class GhostlyPickaxeBakedModel extends BakedModelWrapper {
//
//    public GhostlyPickaxeBakedModel(BakedModel bakedModel) {
//        super(bakedModel);
//    }
//
//    @Override
//    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
//        return transformQuads(super.getQuads(state, side, rand));
//    }
//
//    @Override
//    public List<RenderType> getRenderTypes(ItemStack itemStack, boolean fabulous) {
//        return List.of(AMRenderTypes.getGhostPickaxe(TextureAtlas.LOCATION_BLOCKS));
//    }
//
//    @Override
//    public BakedModel applyTransform(ItemDisplayContext cameraTransformType, PoseStack poseStack, boolean applyLeftHandTransform) {
//        this.getTransforms().getTransform(cameraTransformType).apply(applyLeftHandTransform, poseStack);
//        return this;
//    }
//
//    @Override
//    public List<BakedQuad> getQuads(@org.jetbrains.annotations.Nullable BlockState state, @org.jetbrains.annotations.Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
//        return transformQuads(originalModel.getQuads(state, side, rand, extraData, renderType));
//    }
//
//    private static List<BakedQuad> transformQuads(List<BakedQuad> oldQuads) {
//        List<BakedQuad> quads = new ArrayList<>();
//        for(BakedQuad quad : oldQuads){
//            quads.add(setFullbright(quad));
//        }
//        return quads;
//    }
//
//    private static BakedQuad setFullbright(BakedQuad quad) {
//        int[] vertexData = quad.getVertices().clone();
//        int step = vertexData.length / 4;
//
//        vertexData[6] = 0x00F000F0;
//        vertexData[6 + step] = 0x00F000F0;
//        vertexData[6 + 2 * step] = 0x00F000F0;
//        vertexData[6 + 3 * step] = 0x00F000F0;
//        return new BakedQuad(vertexData, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade());
//    }
//
//    @Override
//    public List<BakedModel> getRenderPasses(ItemStack itemStack, boolean fabulous) {
//        return List.of(this);
//    }
//}
