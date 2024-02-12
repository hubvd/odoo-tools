def patch_arguments(target, attribute, patch_value):
    original_func = getattr(target, attribute)

    def decorator(*args, **kwargs):
        args, kwargs = patch_value(*args, **kwargs)
        return original_func(*args, **kwargs)

    setattr(target, attribute, decorator)


def side_effect(target, attribute, patch_value):
    original_func = getattr(target, attribute)

    def decorator(*args, **kwargs):
        res = original_func(*args, **kwargs)
        patch_value(res, *args, **kwargs)
        return res

    setattr(target, attribute, decorator)
